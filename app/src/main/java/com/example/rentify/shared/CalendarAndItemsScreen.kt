package com.example.rentify.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAndItemsScreen(
    title: String,
    rentedItems: LiveData<List<Map<String, Any>>>,
    rentedDates: LiveData<MutableMap<String, Boolean>>,
    onFetchDatesForItem: (String) -> Unit,
    onBackPress: () -> Unit
) {
    val items = rentedItems.observeAsState(initial = emptyList())
    val dates = rentedDates.observeAsState(initial = mutableMapOf())

    var selectedItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = TextStyle(fontSize = 20.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CustomCalendar(
                rentedDates = dates.value
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Allow the grid to take most of the space
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items.value.size) { index ->
                            val item = items.value[index]
                            val documentId = item["documentId"] as? String
                            ItemCard(
                                item = item,
                                isSelected = selectedItemId == documentId,
                                onClick = {
                                    documentId?.let {
                                        selectedItemId = it // Update the selected item
                                        onFetchDatesForItem(it)
                                    }
                                })
                        }
                    }
                }
            }
        }
    }
}