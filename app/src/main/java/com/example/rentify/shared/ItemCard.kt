package com.example.rentify.shared

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ItemCard(item: Map<String, Any>, isSelected: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clickable { onClick() }
            .let {
                if (isSelected) {
                    it.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    it
                }
            },
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column {
            Box {
                // Image at the top
                AsyncImage(
                    model = item["imageUrl"] as? String ?: "",
                    contentDescription = "Item Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                // Price overlay at the bottom-left of the image
                Text(
                    text = "${item["itemPrice"] ?: "N/A"}",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }

            // Name below the image
            Text(
                text = item["itemName"] as? String ?: "Unknown",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = TextStyle(fontSize = 18.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // First 50 characters of the description
            Text(
                text = (item["itemDescription"] as? String) ?: "No Description",
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                style = TextStyle(fontSize = 14.sp),
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}