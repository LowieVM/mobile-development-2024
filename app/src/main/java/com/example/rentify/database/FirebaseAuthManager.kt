package com.example.rentify.database

//import com.google.api.client.auth.openidconnect.HttpTransportFactory

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject


class FirebaseAuthManager @Inject constructor(private val context: Context) {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun isUserLoggedIn(): Boolean {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val user = mAuth.currentUser
                    user?.let {
                        firestore.collection("users").document(it.uid).update("fcmToken", token)
                    }
                }
            }
        return mAuth.currentUser != null
    }

    fun logoutUser() {
        mAuth.signOut()
    }

    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun registerUser(username: String, email: String, password: String, address: String, latitude: String, longitude: String, onComplete: (Boolean) -> Unit) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = mAuth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(username).build()
                user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        val userId = user.uid
                        val userMap = mapOf(
                            "email" to email,
                            "username" to username,
                            "address" to address,
                            "latitude" to latitude,
                            "longitude" to longitude
                        )

                        firestore.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                logoutUser()
                                onComplete(true)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                onComplete(false)
                            }
                    } else {
                        Toast.makeText(context, "Profile update failed: ${profileTask.exception?.message}", Toast.LENGTH_LONG).show()
                        onComplete(false)
                    }
                }
            } else {
                Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean) -> Unit) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
                                val token = task2.result
                                val user = mAuth.currentUser
                                user?.let {
                                    firestore.collection("users").document(it.uid).update("fcmToken", token)
                                }
                            }
                        }
                    onComplete(true)
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
    }

    fun addItem(itemName: String, itemDescription: String, itemPrice: String, category: String, imageUrl: String?) {
        val user = mAuth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)

            // Fetch user data (including latitude and longitude)
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val latitude = document.getString("latitude") ?: ""
                    val longitude = document.getString("longitude") ?: ""

                    // Create the item data map
                    val itemMap = mutableMapOf(
                        "itemName" to itemName,
                        "itemDescription" to itemDescription,
                        "itemPrice" to itemPrice,
                        "category" to category,
                        "userRef" to userRef,
                        "latitude" to latitude,
                        "longitude" to longitude
                    )

                    // Add image URL if provided
                    imageUrl?.let {
                        itemMap["imageUrl"] = it
                    }

                    // Save the item to Firestore
                    firestore.collection("items").document()
                        .set(itemMap)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Item added successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add item: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "User data not found", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
    }

    fun uploadImageToFirebaseStorage(imageUri: Uri, onComplete: (String?) -> Unit) {
        val user = mAuth.currentUser
        if (user != null) {
            val storageRef: StorageReference = storage.reference
            val imageRef: StorageReference = storageRef.child("images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        onComplete(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(null)
                }
        } else {
            onComplete(null)
        }
    }



    fun fetchAllItems(onComplete: (List<Map<String, Any>>) -> Unit) {
        firestore.collection("items").get()
            .addOnSuccessListener { querySnapshot ->
                val items = querySnapshot.documents.mapNotNull { document ->
                    val data = document.data
                    if (data != null) {
                        // Add the document ID to the map
                        data["documentId"] = document.id
                        data
                    } else {
                        null
                    }
                }
                onComplete(items)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }



    fun fetchUserItems(onComplete: (List<Map<String, Any>>) -> Unit) {
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        val userRef = firestore.collection("users").document(currentUser.uid)

        // Query items collection where 'owner' matches the logged-in user's reference
        firestore.collection("items")
            .whereEqualTo("userRef", userRef)
            .get()
            .addOnSuccessListener { itemsSnapshot ->
                val items = itemsSnapshot.documents.mapNotNull { it.data?.apply { put("documentId", it.id) } }
                onComplete(items)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch user items", Toast.LENGTH_LONG).show()
                onComplete(emptyList())
            }
    }

    fun fetchUserRentedItems(onComplete: (List<Map<String, Any>>) -> Unit) {
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        val userRef = firestore.collection("users").document(currentUser.uid)

        // Query rentals collection where userRef matches the logged-in user
        firestore.collection("rentals")
            .whereEqualTo("userRef", userRef)
            .get()
            .addOnSuccessListener { rentalSnapshot ->
                val itemRefs = rentalSnapshot.documents.mapNotNull { it.getDocumentReference("itemRef") }

                if (itemRefs.isEmpty()) {
                    onComplete(emptyList())
                    return@addOnSuccessListener
                }

                // Fetch details of items referenced by itemRefs
                firestore.collection("items")
                    .whereIn(FieldPath.documentId(), itemRefs.map { it.id })
                    .get()
                    .addOnSuccessListener { itemsSnapshot ->
                        val items = itemsSnapshot.documents.mapNotNull { it.data?.apply { put("documentId", it.id) } }
                        onComplete(items)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to fetch item details", Toast.LENGTH_LONG).show()
                        onComplete(emptyList())
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch rentals", Toast.LENGTH_LONG).show()
                onComplete(emptyList())
            }
    }


    fun rentItem(documentId: String, dates: Map<String, Boolean>, onComplete: (Boolean) -> Unit) {
        val user = mAuth.currentUser
        if (user != null) {
            val userRef = firestore.collection("users").document(user.uid)
            val itemRef = firestore.collection("items").document(documentId)

            // Extract the keys (dates) from the dates map
            val dateArray = dates.keys.toList()

            // Create a rental data map
            val rentalData = mapOf(
                "itemRef" to itemRef,
                "userRef" to userRef,
                "dates" to dateArray
            )

            // Save the rental document to Firestore
            firestore.collection("rentals").document()
                .set(rentalData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item rented successfully!", Toast.LENGTH_SHORT).show()

                    itemRef.get().addOnSuccessListener { document ->
                        val ownerId = document.getDocumentReference("userRef")?.id
                        if (ownerId != null) {
                            firestore.collection("users").document(ownerId).get()
                                .addOnSuccessListener { userDoc ->
                                    val ownerToken = userDoc.getString("fcmToken")
                                    if (ownerToken != null) {

                                        getAccessTokenFromAssetsAsync("rentify-key.json") { accessToken ->
                                            if (accessToken != null) {
                                                // Once accessToken is received, send the notification
                                                sendNotificationToOwner(ownerToken, "Your item has been rented!", accessToken)
                                            } else {
                                                // Handle the case where the access token is null (failed to fetch)
                                                Toast.makeText(context, "Failed to get access token.", Toast.LENGTH_LONG).show()
                                            }
                                        }

                                    }
                                }
                        }
                    }

                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to rent item: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
    }

    val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")
    fun getAccessTokenFromAssetsAsync(serviceAccountFileName: String, onComplete: (String?) -> Unit) {
        // Launch the coroutine in the background (IO context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Access the service account JSON from assets
                val inputStream: InputStream = context.assets.open(serviceAccountFileName)

                // Load the credentials from the InputStream
                val credentials = ServiceAccountCredentials.fromStream(inputStream)

                // You don't need HttpTransportFactory.create here
                val credentialsWithScopes = credentials.createScoped(SCOPES)

                // Refresh the access token
                val accessToken = credentialsWithScopes.refreshAccessToken()

                // Call the completion callback with the access token on the main thread
                withContext(Dispatchers.Main) {
                    onComplete(accessToken.tokenValue)
                }
            } catch (e: IOException) {
                e.printStackTrace()

                // Call the completion callback with null on error
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    // Function to send notification
    fun sendNotificationToOwner(token: String, message: String, accessToken: String) {
        val json = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "Item Rented",
              "body": "$message"
            }
          }
        }
    """.trimIndent()

        val client = OkHttpClient()
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/v1/projects/rentify-89fc3/messages:send")
            .addHeader("Authorization", "Bearer $accessToken")  // Use OAuth 2.0 Bearer token
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Notification sent successfully")
                } else {
                    println("Error sending notification: ${response.body?.string()}")
                }
            }
        })
    }

    fun fetchDatesFromItem(documentId: String?, onComplete: (List<String>) -> Unit) {
        if (documentId != null) {
            val itemRef = firestore.collection("items").document(documentId)

            firestore.collection("rentals")
                .whereEqualTo("itemRef", itemRef)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Collect all dates from the rental documents
                    val dates = mutableSetOf<String>()
                    for (document in querySnapshot.documents) {
                        val rentedDates = document.get("dates") as? List<String>
                        rentedDates?.let {
                            dates.addAll(it)
                        }
                    }
                    onComplete(dates.toList())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to fetch dates: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete(emptyList())
                }
        }
    }

    fun fetchDatesFromRentedItem(documentId: String?, onComplete: (List<String>) -> Unit) {
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            onComplete(emptyList())
            return
        }

        if (documentId != null) {
            val itemRef = firestore.collection("items").document(documentId)
            val userRef = firestore.collection("users").document(currentUser.uid)

            firestore.collection("rentals")
                .whereEqualTo("itemRef", itemRef) // Filter by item
                .whereEqualTo("userRef", userRef) // Filter by current user
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Collect all dates from the rental documents
                    val dates = mutableSetOf<String>()
                    for (document in querySnapshot.documents) {
                        val rentedDates = document.get("dates") as? List<String>
                        rentedDates?.let {
                            dates.addAll(it)
                        }
                    }
                    onComplete(dates.toList())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to fetch dates: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete(emptyList())
                }
        }
    }


}

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            // Display the notification
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder = NotificationCompat.Builder(this, "default_channel")
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            notificationManager.notify(0, notificationBuilder.build())
        }
    }
}