package com.example.rentify.shared

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

@SuppressLint("RememberReturnType")
@Composable
fun CustomCalendar(
    rentedDates: MutableMap<String, Boolean>? = null,
    selectedDates: MutableMap<String, Boolean>? = null,
    disabledDates: Map<String, Boolean>? = null,
    onDateSelected: ((String) -> Unit)? = null
) {
    val currentDateInstance = Calendar.getInstance()

    // State for the currently displayed month and year
    var month by remember { mutableIntStateOf(currentDateInstance.get(Calendar.MONTH)) }
    var year by remember { mutableIntStateOf(currentDateInstance.get(Calendar.YEAR)) }

    val minSelectableDate = remember { currentDateInstance.timeInMillis }

    // Get the first day of the month and the number of days in the month
    val currentDate = remember { Calendar.getInstance() }
    currentDate.set(year, month, 1)

    val today = Calendar.getInstance()

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

                val isRented = rentedDates?.get(dayString) ?: false
                val isSelected = selectedDates?.get(dayString) ?: false
                val isDisabled = disabledDates?.get(dayString) ?: false

                // Check if the date is today
                val isToday = (day == today.get(Calendar.DAY_OF_MONTH)
                        && month == today.get(Calendar.MONTH)
                        && year == today.get(Calendar.YEAR))

                item {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .background(
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isRented -> MaterialTheme.colorScheme.primaryContainer
                                    isBeforeMinDate || isDisabled -> Color.LightGray // Disable previous dates
                                    else -> Color.Transparent
                                },
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable(enabled = !isBeforeMinDate && !isDisabled && selectedDates != null) {
                                if (onDateSelected != null) {
                                    if (!isSelected) {
                                        selectedDates?.put(dayString, true)
                                    } else {
                                        selectedDates?.remove(dayString)
                                    }
                                    onDateSelected(dayString)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = TextStyle(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                textDecoration = if (isToday) TextDecoration.Underline else TextDecoration.None
                            ),
                            color = when {
                                isSelected -> Color.White
                                isRented -> Color.White
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

