package com.azuratech.azuratime.features.attendance.ui.manual

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.core.ui.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ManualAttendanceContent(
    selectedFace: StudentBiometricDetails?,
    onFaceSelected: (StudentBiometricDetails?) -> Unit,
    faces: List<StudentBiometricDetails>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    selectedClass: ClassModel?,
    onClassSelected: (ClassModel?) -> Unit,
    availableClasses: List<ClassModel?>,
    isLocked: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    AzuraScreen(
        title = "Manual Input",
        onBack = onBack,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AzuraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                if (isLocked) {
                    ManualAttendanceLockBanner()
                }

                // 1. Student Selection
                ManualAttendanceFilterDropdown(
                    label = "Select Student",
                    options = faces,
                    selectedOption = selectedFace,
                    onOptionSelected = onFaceSelected,
                    getLabel = { it.biometric.name },
                    enabled = !isLocked,
                )

                // 2. Date & Time Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                    if (isLocked) {
                        OutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.medium,
                        )
                    } else {
                        AzuraDatePickerButton(
                            label = "Date",
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    ManualAttendanceTimePickerButton(
                        label = "Time",
                        selectedTime = selectedTime,
                        onTimeSelected = onTimeSelected,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(AzuraSpacing.sm))

                // 3. Attendance Status
                Text("Attendance Status", style = MaterialTheme.typography.labelLarge)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                ) {
                    listOf("H" to "Present", "T" to "Late", "S" to "Sick", "I" to "Excused", "A" to "Absent").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStatus == code,
                            onClick = { onStatusSelected(code) },
                            label = { Text(label, textAlign = TextAlign.Center) },
                            shape = AzuraShapes.small,
                        )
                    }
                }

                Spacer(Modifier.height(AzuraSpacing.sm))

                // 4. Session/Class Dropdown
                ManualAttendanceFilterDropdown(
                    label = "Select Session/Class (Optional)",
                    options = availableClasses,
                    selectedOption = selectedClass,
                    onOptionSelected = onClassSelected,
                    getLabel = { it?.name ?: "General / No Class" },
                )

                Spacer(Modifier.weight(1f))

                // 5. Save Action
                Button(
                    onClick = onSave,
                    enabled = selectedFace != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AzuraSpacing.lg)
                        .height(56.dp),
                    shape = AzuraShapes.medium,
                ) {
                    Text("Save Attendance", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
