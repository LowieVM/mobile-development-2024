package com.example.rentify.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.rentify.database.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {

    fun addItem(itemName: String, itemDescription: String, itemPrice: String, category: String, imageUri: Uri?) {
        if (imageUri != null) {
            firebaseAuthManager.uploadImageToFirebaseStorage(imageUri) { imageUrl ->
                if (imageUrl != null) {
                    firebaseAuthManager.addItem(itemName, itemDescription, itemPrice, category, imageUrl)
                } else {
                }
            }
        } else {
            firebaseAuthManager.addItem(itemName, itemDescription, itemPrice, category, null)
        }
    }
}

