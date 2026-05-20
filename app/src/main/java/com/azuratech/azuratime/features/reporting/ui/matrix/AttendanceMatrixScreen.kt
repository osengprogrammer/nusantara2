package com.azuratech.azuratime.features.reporting.ui.matrix

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.reporting.ui.components.MatrixTableView

@Composable
fun AttendanceMatrixScreen(
    onNavigateBack: () -> Unit,
    onCellClick: (String, String, java.time.LocalDate) -> Unit,
    viewModel: AttendanceMatrixViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Matriks Presensi",
        onBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AzuraSpacing.md),
        ) {
            when (val state = uiState) {
                is AttendanceMatrixUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AttendanceMatrixUiState.Success -> {
                    // 🔍 AI Native: Filter Controls (MVI Compliant)
                    AttendanceMatrixFilters(
                        data = state.data,
                        onEvent = viewModel::onEvent,
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        MatrixTableView(
                            data = state.data,
                            onCellClick = onCellClick,
                        )
                    }
                }
                is AttendanceMatrixUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * 🔍 ATTENDANCE MATRIX FILTERS
 * MVI Component for handling user input and triggering matrix updates.
 */
@Composable
private fun AttendanceMatrixFilters(
    data: AttendanceMatrixData,
    onEvent: (AttendanceMatrixUiEvent) -> Unit,
) {
    var isClassExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md),
    ) {
        // 1. Sleek Search Bar (Identical to History Log)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AzuraSpacing.xs),
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = data.searchQuery,
                onValueChange = { onEvent(AttendanceMatrixUiEvent.Search(it)) },
                placeholder = { Text("Cari nama siswa...") },
                modifier = Modifier.fillMaxWidth(),
                shape = com.azuratech.azuratime.core.ui.theme.AzuraShapes.medium,
                leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (data.searchQuery.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = { onEvent(AttendanceMatrixUiEvent.Search("")) }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Close, null)
                        }
                    }
                },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
                singleLine = true,
            )
        }

        Spacer(modifier = Modifier.height(AzuraSpacing.sm))

        // 2. Class Dropdown
        val selectedClassName = data.availableClasses.find { it.id == data.selectedClassId }?.name ?: "Semua Kelas"
        AzuraDropdownField(
            label = "Filter Kelas",
            selectedValue = selectedClassName,
            options = listOf(null) + data.availableClasses,
            isExpanded = isClassExpanded,
            onExpandedChange = { isClassExpanded = it },
            onOptionSelected = { classModel ->
                onEvent(AttendanceMatrixUiEvent.FilterByClass(classModel?.id ?: ""))
            },
            getOptionLabel = { it?.name ?: "Semua Kelas" },
        )

        Spacer(modifier = Modifier.height(AzuraSpacing.sm))

        // 3. Date Range Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AzuraDatePickerButton(
                label = "Mulai",
                selectedDate = data.startDate,
                onDateSelected = { onEvent(AttendanceMatrixUiEvent.FilterByDate(it, data.endDate)) },
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(AzuraSpacing.sm))

            AzuraDatePickerButton(
                label = "Sampai",
                selectedDate = data.endDate,
                onDateSelected = { onEvent(AttendanceMatrixUiEvent.FilterByDate(data.startDate, it)) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(AzuraSpacing.md))
    }
}
