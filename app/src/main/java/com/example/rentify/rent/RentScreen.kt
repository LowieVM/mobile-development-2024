package com.example.rentify.rent

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rentify.shared.ItemCard

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RentScreen(viewModel: RentViewModel = viewModel(), context: Context) {
    val items = viewModel.items.observeAsState(initial = emptyList())
    val state by viewModel.state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = viewModel::fetchItems
    )
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("All Categories", "Bouwgereedschap", "Keukenapparatuur", "Schoonmaakapparatuur", "Transportbenodigdheden", "Tuinbenodigdheden")
    var selectedCategory by remember { mutableStateOf("All Categories") }

    val filteredItems = if (selectedCategory != "All Categories") {
        items.value.filter { item ->
            val itemCategory = item["category"] as? String ?: ""
            itemCategory == selectedCategory
        }
    } else {
        items.value // Show all items if "All Categories" is selected
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rent") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDropdownExpanded = !isDropdownExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start,
                            color = if (selectedCategory.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isDropdownExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isDropdownExpanded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp), // Fixed height for dropdown
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            LazyColumn {
                                items(categories.size) { index ->
                                    val category = categories[index]
                                    Text(
                                        text = category,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategory = category
                                                isDropdownExpanded = false
                                            }
                                            .padding(8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Allow the grid to take most of the space
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems.size) { index ->
                        val item = filteredItems[index]
                        ItemCard(item = item, onClick = {
                            // Start the activity and pass the item data
                            val intent = Intent(context, RentItemActivity::class.java)
                            intent.putExtra("itemName", item["itemName"] as? String ?: "")
                            intent.putExtra("itemPrice", item["itemPrice"] as? String ?: "")
                            intent.putExtra("itemDescription", item["itemDescription"] as? String ?: "")
                            intent.putExtra("imageUrl", item["imageUrl"] as? String ?: "")
                            intent.putExtra("documentId", item["documentId"] as? String ?: "")
                            context.startActivity(intent)
                        })
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Adjust the height as needed
            }

            PullRefreshIndicator(
                refreshing = viewModel.state.value.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}