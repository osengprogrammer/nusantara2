package com.azuratech.azuratime.ui.report.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.core.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraDropdownField
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes
import java.time.LocalDate

@Composable
fun ReportFilterSection(
    startDate: LocalDate,
    endDate: LocalDate,
    searchQuery: String,
    selectedClassId: String?,
    availableClasses: List<ClassModel>,
    onSearchChange: (String) -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onClassSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isClassExpanded by remember { mutableStateOf(false) }
    val selectedClassName = availableClasses.find { it.id == selectedClassId }?.name ?: "Semua Kelas"

    Column(modifier = modifier.padding(AzuraSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
            AzuraDatePickerButton(
                label = "Dari",
                selectedDate = startDate,
                onDateSelected = { onDateRangeSelected(it, endDate) },
                modifier = Modifier.weight(1f)
            )
            AzuraDatePickerButton(
                label = "Sampai",
                selectedDate = endDate,
                onDateSelected = { onDateRangeSelected(startDate, it) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AzuraSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari nama...") },
                modifier = Modifier.weight(1f),
                shape = AzuraShapes.medium,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.alpha(0.3f)) }
            )
            AzuraDropdownField(
                label = "Kelas",
                selectedValue = selectedClassName,
                options = listOf(ClassModel(id = "ALL", schoolId = "", name = "Semua Kelas", grade = "", teacherId = null, createdAt = 0L)) + availableClasses,
                isExpanded = isClassExpanded,
                onExpandedChange = { isClassExpanded = it },
                onOptionSelected = {
                    onClassSelected(it.id)
                    isClassExpanded = false
                },
                getOptionLabel = { it.name },
                onEditClicked = { }
            )
        }
    }
}
