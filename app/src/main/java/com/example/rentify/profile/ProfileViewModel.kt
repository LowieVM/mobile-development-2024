package com.example.rentify.profile

import androidx.lifecycle.ViewModel
import com.example.rentify.database.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {
    fun logoutUser() {
        firebaseAuthManager.logoutUser()
    }
}
