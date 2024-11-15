package com.example.rentify.database

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
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

    fun getUserName(): String? {
        return mAuth.currentUser?.displayName
    }


    fun addItem(itemName: String, itemDescription: String, itemPrice: String, imageUrl: String?) {
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
}
