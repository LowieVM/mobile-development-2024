package com.example.rentify.database

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class FirebaseAuthManager @Inject constructor(private val context: Context) {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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


    fun addItem(itemName: String, itemDescription: String, itemPrice: String) {
        val user = mAuth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)

            val userMap = mapOf(
                "itemName" to itemName,
                "itemDescription" to itemDescription,
                "itemPrice" to itemPrice,
                "userRef" to userRef
            )

            firestore.collection("items").document()
                .set(userMap)
                .addOnSuccessListener {
                    Toast.makeText(context, "Item added successful!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add item: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
    }
}
