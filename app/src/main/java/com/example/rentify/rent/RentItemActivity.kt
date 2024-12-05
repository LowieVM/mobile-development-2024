package com.example.rentify.rent

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rentify.database.FirebaseAuthManager
import com.example.rentify.shared.CustomCalendar
import com.example.rentify.ui.theme.RentifyTheme

class RentItemActivity : ComponentActivity() {
    private lateinit var firebaseAuthManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuthManager = FirebaseAuthManager(this)

        // Retrieve data passed from the previous activity
        val itemName = intent.getStringExtra("itemName")
        val itemPrice = intent.getStringExtra("itemPrice")
        val itemDescription = intent.getStringExtra("itemDescription")
        val imageUrl = intent.getStringExtra("imageUrl")
        val documentId = intent.getStringExtra("documentId")
        val ownerId = intent.getStringExtra("ownerId") // Assume ownerId is passed

        val currentUserId = firebaseAuthManager.getCurrentUserId()


        // Create a mutable state map for disabled dates
        val disabledDates = mutableStateMapOf<String, Boolean>()

        // Fetch disabled dates from Firestore
        firebaseAuthManager.fetchDatesFromItem(documentId) { dates ->
            dates.forEach { date ->
                disabledDates[date] = true
            }
        }



        setContent {
            RentifyTheme {
                RentItemScreen(
                    itemName = itemName ?: "Unknown Item",
                    itemPrice = itemPrice ?: "N/A",
                    itemDescription = itemDescription ?: "No description available.",
                    imageUrl = imageUrl ?: "",
                    documentId = documentId ?: "",
                    onBackPress = { finish() },
                    onRentItem = { documentId, dates ->
                        if (currentUserId == ownerId) {
                            // Show dialog if the current user is the owner
                            // Show a confirmation dialog to warn the user that they are renting their own item
                            val dialog = AlertDialog.Builder(this)
                                .setTitle("Warning")
                                .setMessage("You are the owner of this item. Are you sure you want to rent your own item?")
                                .setPositiveButton("OK") { dialog, _ ->
                                    // Proceed with renting
                                    firebaseAuthManager.rentItem(documentId, dates) { success ->
                                        if (success) {
                                            finish()
                                        }
                                    }
                                    dialog.dismiss()
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()

                            dialog.show()
                        } else {
                            // Proceed with renting
                            firebaseAuthManager.rentItem(documentId, dates) { success ->
                                if (success) {
                                    finish()
                                }
                            }
                        }
                    },
                    disabledDates = disabledDates
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentItemScreen(
    itemName: String,
    itemPrice: String,
    itemDescription: String,
    imageUrl: String,
    documentId: String,
    onBackPress: () -> Unit,
    onRentItem: (documentId: String, dates: Map<String, Boolean>) -> Unit,
    disabledDates: Map<String, Boolean>
) {

    var lastSelectedDate by remember { mutableStateOf<String?>(null) }
    val selectedDates = remember { mutableStateMapOf<String, Boolean>() }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = itemName,
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Item image at the top
            AsyncImage(
                model = imageUrl,
                contentDescription = "Item Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(250.dp, 500.dp),
                contentScale = ContentScale.Fit
            )

            // Item description
            Text(
                text = itemDescription,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                style = TextStyle(fontSize = 16.sp, color = Color.Gray),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            // Item price at the bottom
            Text(
                text = "Price: $itemPrice",
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )



            CustomCalendar(
                selectedDates = selectedDates,
                disabledDates = disabledDates,
                onDateSelected = { date -> lastSelectedDate = date }
            )

            Button(
                onClick = {
                    if (selectedDates.isNotEmpty()) {
                        onRentItem(documentId, selectedDates);
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedDates.isNotEmpty()
            ) {
                Text("Rent Item")
            }
        }
    }
}
