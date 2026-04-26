package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuratime.ui.core.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ManualAttendanceContent(
    selectedFace: FaceWithDetails?,
    onFaceSelected: (FaceWithDetails?) -> Unit,
    faces: List<FaceWithDetails>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    selectedClass: ClassModel?,
    onClassSelected: (ClassModel?) -> Unit,
    availableClasses: List<ClassModel>,
    isLocked: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    AzuraScreen(
        title = "Input Manual",
        onBack = onBack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AzuraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                if (isLocked) {
                    ManualAttendanceLockBanner()
                }

                // 1. Personnel Selection
                ManualAttendanceFilterDropdown(
                    label = "Pilih Personil",
                    options = faces,
                    selectedOption = selectedFace,
                    onOptionSelected = onFaceSelected,
                    getLabel = { it.face.name },
                    enabled = !isLocked
                )

                // 2. Date & Time Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                    if (isLocked) {
                        OutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            onValueChange = {},
                            label = { Text("Tanggal") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.medium
                        )
                    } else {
                        AzuraDatePickerButton(
                            label = "Tanggal",
                            selectedDate = selectedDate,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ManualAttendanceTimePickerButton(
                        label = "Jam",
                        selectedTime = selectedTime,
                        onTimeSelected = onTimeSelected,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(AzuraSpacing.sm))

                // 3. Attendance Status
                Text("Status Kehadiran", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                    listOf("H" to "Hadir", "S" to "Sakit", "I" to "Izin", "A" to "Alpa").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStatus == code,
                            onClick = { onStatusSelected(code) },
                            label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.small
                        )
                    }
                }

                Spacer(Modifier.height(AzuraSpacing.sm))

                // 4. Session/Class Dropdown
                ManualAttendanceFilterDropdown(
                    label = "Pilih Sesi/Kelas",
                    options = availableClasses,
                    selectedOption = selectedClass,
                    onOptionSelected = onClassSelected,
                    getLabel = { it.name }
                )

                Spacer(Modifier.weight(1f))

                // 5. Save Action
                Button(
                    onClick = onSave,
                    enabled = selectedFace != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = AzuraSpacing.lg),
                    shape = AzuraShapes.medium
                ) {
                    Text("Simpan Kehadiran", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
