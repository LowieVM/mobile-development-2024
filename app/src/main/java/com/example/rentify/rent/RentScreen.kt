package com.example.rentify.rent

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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