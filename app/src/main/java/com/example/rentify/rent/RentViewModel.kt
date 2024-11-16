package com.example.rentify.rent

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rentify.database.FirebaseAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RentViewModel @Inject constructor(
    private val firebaseAuthManager: FirebaseAuthManager
) : ViewModel() {
    private val _state = mutableStateOf(RentScreenState())
    val state: State<RentScreenState> = _state

    private val _items = MutableLiveData<List<Map<String, Any>>>()
    val items: LiveData<List<Map<String, Any>>> get() = _items

    init {
        fetchItems()
    }

    fun fetchItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)
            delay(1_000L)
            firebaseAuthManager.fetchAllItems { fetchedItems ->
                _items.postValue(fetchedItems)
            }
            _state.value = _state.value.copy(
                isLoading = false,
            )
        }
    }
}

data class RentScreenState(
    val isLoading: Boolean = false,
    val items: List<Map<String, Any>> = emptyList()
)
