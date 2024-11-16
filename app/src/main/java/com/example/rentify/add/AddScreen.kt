package com.example.rentify.add

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onAddItem: (itemName: String, itemDescription: String, itemPrice: String, imageUri: Uri?) -> Unit
) {
    val permissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)

    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var imageUri: Uri? by remember { mutableStateOf(null) }

    LaunchedEffect(permissionState) {
        permissionState.launchPermissionRequest()
    }

    val isFormValid = itemName.isNotEmpty() && itemDescription.isNotEmpty() && itemPrice.isNotEmpty() && imageUri != null

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    fun resetForm() {
        itemName = ""
        itemDescription = ""
        itemPrice = ""
        imageUri = null
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        TopAppBar(
            title = { Text("Add item") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = itemDescription, onValueChange = { itemDescription = it }, label = { Text("Description") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = itemPrice, onValueChange = { itemPrice = it }, label = { Text("Price") })
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text("Select Image")
            }

            if (imageUri != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }

            if (imageUri == null) {
                Text(
                    text = "Please select an image.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!isFormValid) {
                Text(
                    text = "Please fill in all fields and select an image.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onAddItem(itemName, itemDescription, itemPrice, imageUri)

                        resetForm()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text("Add Item")
            }
        }
    }
}
