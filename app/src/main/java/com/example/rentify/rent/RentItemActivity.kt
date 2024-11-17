package com.example.rentify.rent

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import java.util.Calendar
import java.util.Locale

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


        // Create a mutable state map for disabled dates
        val disabledDates = mutableStateMapOf<String, Boolean>()

        // Fetch disabled dates from Firestore
        firebaseAuthManager.fetchDatesFromItem(documentId) { dates ->
            dates.forEach { date ->
                disabledDates[date] = true
            }
        }



        setContent {
            RentItemScreen(
                itemName = itemName ?: "Unknown Item",
                itemPrice = itemPrice ?: "N/A",
                itemDescription = itemDescription ?: "No description available.",
                imageUrl = imageUrl ?: "",
                documentId = documentId ?: "",
                onBackPress = { finish() },
                onRentItem = { documentId, dates ->
                    firebaseAuthManager.rentItem(documentId, dates) { success ->
                        if (success) {
                            finish()
                        }
                    }
                },
                disabledDates = disabledDates
            )
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
    //val disabledDates = remember { mutableStateMapOf<String, Boolean>() }


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



            CustomCalendarMonth(
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

@SuppressLint("RememberReturnType")
@Composable
fun CustomCalendarMonth(
    selectedDates: MutableMap<String, Boolean>,
    disabledDates: Map<String, Boolean>,
    onDateSelected: (String) -> Unit
) {
    val currentDateInstance = Calendar.getInstance()

    // State for the currently displayed month and year
    var month by remember { mutableIntStateOf(currentDateInstance.get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(currentDateInstance.get(Calendar.YEAR)) }

    val minSelectableDate = remember { currentDateInstance.timeInMillis }

    // Get the first day of the month and the number of days in the month
    val currentDate = remember { Calendar.getInstance() }
    currentDate.set(year, month, 1)

    // Update calendar calculations when month or year changes
    currentDate.firstDayOfWeek = Calendar.MONDAY
    val firstDayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Function to format date as dd/MM/yyyy
    fun formatDate(day: Int): String {
        currentDate.set(Calendar.DAY_OF_MONTH, day)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(currentDate.time)
    }

    // Adjusting the first day of the week to Monday
    val adjustedFirstDayOfWeek = if (firstDayOfWeek == Calendar.SUNDAY) 7 else firstDayOfWeek - 1

    Column {
        // Navigation buttons
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            IconButton(onClick = {
                // Go to the previous month
                if (month == 0) {
                    month = 11
                    year -= 1
                } else {
                    month -= 1
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }

            Text(
                text = "${currentDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} $year",
                style = TextStyle(fontSize = 22.sp)
            )

            IconButton(onClick = {
                // Go to the next month
                if (month == 11) {
                    month = 0
                    year += 1
                } else {
                    month += 1
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
                .heightIn(300.dp, 350.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val daysOfWeek = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
            items(daysOfWeek.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(daysOfWeek[index])
                }
            }


            // Add empty spaces for days before the 1st of the month
            items(adjustedFirstDayOfWeek - 1) {
                Text("")
            }

            // Add the days of the month
            for (day in 1..daysInMonth) {
                val dayString = formatDate(day)

                // Determine if the current date is selectable
                val isBeforeMinDate = minSelectableDate.let {
                    val currentDayTime = currentDate.apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
                    currentDayTime < it
                }

                val isSelected = selectedDates[dayString] ?: false
                val isDisabled = disabledDates[dayString] ?: false

                item {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .background(
                                color = when {
                                    isSelected -> Color.Cyan
                                    isBeforeMinDate || isDisabled -> Color.LightGray // Disable previous dates
                                    else -> Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable(enabled = !isBeforeMinDate && !isDisabled) { // Disable clicks for previous dates
                                if (!isSelected) {
                                    selectedDates[dayString] = true
                                } else {
                                    selectedDates.remove(dayString)
                                }
                                onDateSelected(dayString)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = when {
                                isSelected -> Color.White
                                isBeforeMinDate || isDisabled -> Color.Gray // Adjust text color for disabled dates
                                else -> Color.Black
                            }
                        )
                    }
                }
            }
        }
    }
}