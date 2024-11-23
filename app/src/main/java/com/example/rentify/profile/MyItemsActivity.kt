package com.example.rentify.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.rentify.database.FirebaseAuthManager
import com.example.rentify.shared.CalendarAndItemsScreen

class MyItemsActivity : ComponentActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager

    private val _items = MutableLiveData<List<Map<String, Any>>>()
    private val items: LiveData<List<Map<String, Any>>> get() = _items

    private val _rentedDates = MutableLiveData<MutableMap<String, Boolean>>()
    private val rentedDates: LiveData<MutableMap<String, Boolean>> get() = _rentedDates
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuthManager = FirebaseAuthManager(this)

        fetchItems()

        setContent {
            CalendarAndItemsScreen(
                title = "Your items",
                rentedItems = items,
                rentedDates = rentedDates,
                onFetchDatesForItem = { documentId ->
                    fetchDatesForItem(documentId)
                },
                onBackPress = { finish() }
            )
        }
    }

    private fun fetchItems() {
        firebaseAuthManager.fetchUserItems { fetchedItems ->
            _items.postValue(fetchedItems)
        }
    }

    private fun fetchDatesForItem(documentId: String) {
        firebaseAuthManager.fetchDatesFromItem(documentId) { dates ->
            val newDates = mutableMapOf<String, Boolean>()
            dates.forEach { date ->
                newDates[date] = true
            }
            _rentedDates.postValue(newDates)
        }
    }
}

