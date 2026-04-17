package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraDatePickerButton(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    
    // Initialize the date picker state with the currently selected date (if any)
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    OutlinedButton(
        modifier = modifier, 
        shape = AzuraShapes.medium, // 🔥 System Shapes for consistent borders
        onClick = { showPicker = true }
    ) {
        // 🔥 Added a calendar icon for a much better UX
        Icon(
            imageVector = Icons.Default.CalendarToday, 
            contentDescription = "Select Date",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(AzuraSpacing.sm)) // 🔥 System Spacing
        
        Text(selectedDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: label)
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            shape = AzuraShapes.large, // 🔥 System Shapes for a premium floating dialog
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let {
                            val localDate = Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(localDate)
                        }
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) { 
            DatePicker(state = dateState) 
        }
    }
}