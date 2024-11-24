package com.example.rentify.database

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID
import javax.inject.Inject

class FirebaseAuthManager @Inject constructor(private val context: Context) {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun isUserLoggedIn(): Boolean {
        return mAuth.currentUser != null
    }

    fun logoutUser() {
        mAuth.signOut()
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
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to rent item: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
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


}
