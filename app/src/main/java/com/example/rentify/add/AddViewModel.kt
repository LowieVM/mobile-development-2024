package com.example.rentify.add

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

}
