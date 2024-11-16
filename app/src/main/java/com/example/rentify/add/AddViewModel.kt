package com.example.rentify.add

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rentify.database.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    init {
        _userName.value = firebaseAuthManager.getUserName() ?: "User"
    }

    fun addItem(itemName: String, itemDescription: String, itemPrice: String, imageUri: Uri?) {
        if (imageUri != null) {
            firebaseAuthManager.uploadImageToFirebaseStorage(imageUri) { imageUrl ->
                if (imageUrl != null) {
                    firebaseAuthManager.addItem(itemName, itemDescription, itemPrice, imageUrl)
                } else {
                }
            }
        } else {
            firebaseAuthManager.addItem(itemName, itemDescription, itemPrice, null)
        }
    }
}

