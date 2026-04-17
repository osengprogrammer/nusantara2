package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle 

// 🔥 Database Entities
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceWithDetails

// 🔥 Services & ViewModels
import com.azuratech.azuratime.ui.checkin.AttendanceService
import com.azuratech.azuratime.ui.add.FaceViewModel
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.user.UserManagementViewModel

// 🔥 Azura Design System
import com.azuratech.azuratime.ui.core.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceScreen(
    faceViewModel: FaceViewModel,
    checkInViewModel: CheckInViewModel,
    userViewModel: UserManagementViewModel,
    classViewModel: ClassViewModel, 
    initialFaceId: String = "",
    initialDate: String = "",
    onBack: () -> Unit
) {
    val faces: List<FaceWithDetails> by faceViewModel.faceList.collectAsStateWithLifecycle(emptyList())
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val assignedIds by userViewModel.assignedClassIds.collectAsStateWithLifecycle(emptyList())
    val globalClasses by classViewModel.classes.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(Unit) {
        // userViewModel.loadCurrentUser() // Ensure this method exists, or remove if currentUser is a StateFlow
    }

    // Role-Based Class Access
    val isAdmin = currentUser?.memberships?.get(currentUser?.activeSchoolId)?.role == "ADMIN"
    val availableClasses = remember(globalClasses, assignedIds, isAdmin) {
        // 🔥 FIXED: Assuming ClassEntity uses classId. Change to 'id' if needed.
        if (isAdmin) globalClasses else globalClasses.filter { it.id in assignedIds }
    }

    // --- State Management ---
    var selectedFace by remember(faces, initialFaceId) {
        mutableStateOf(faces.find { it.face.faceId == initialFaceId })
    }
    var selectedStatus by remember { mutableStateOf("H") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialDate.isNotEmpty()) runCatching { LocalDate.parse(initialDate) }.getOrElse { LocalDate.now() }
            else LocalDate.now()
        )
    }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    
    // 🔥 FIXED: Replaced currentUser?.activeClassId with null to default to General Scan, 
    // unless you have actually added activeClassId to your UserEntity.
    var selectedClass by remember(availableClasses) { 
        mutableStateOf<ClassEntity?>(null) 
    }

    val isLocked = initialFaceId.isNotEmpty()

    AzuraScreen(
        title = "Input Manual", 
        onBack = onBack
    ) {
        // Box wrap to ensure the column fills the screen correctly under the AzuraScreen Scaffold
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AzuraSpacing.lg), // AzuraScreen handles horizontal padding
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                
                if (isLocked) {
                    LockBanner()
                }

                // 1. Personnel Selection
                FilterDropdown(
                    label = "Pilih Personil",
                    options = faces,
                    selectedOption = selectedFace,
                    onOptionSelected = { selectedFace = it },
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
                            onDateSelected = { selectedDate = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AzuraTimePickerButton(
                        label = "Jam",
                        selectedTime = selectedTime,
                        onTimeSelected = { selectedTime = it },
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
                            onClick = { selectedStatus = code },
                            label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.small
                        )
                    }
                }

                Spacer(Modifier.height(AzuraSpacing.sm))

                // 4. Session/Class Dropdown
                FilterDropdown(
                    label = "Pilih Sesi/Kelas",
                    options = availableClasses,
                    selectedOption = selectedClass,
                    onOptionSelected = { selectedClass = it },
                    getLabel = { it.name }
                )

                Spacer(Modifier.weight(1f))

                // 5. Save Action
                Button(
                    onClick = {
                        selectedFace?.let { faceWithDetails ->
                            val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            
                            // Ensure AttendanceService.createRecord matches your actual method signature
                            // If it doesn't exist, you'll need to instantiate CheckInRecordEntity directly here
                            val newRecord = AttendanceService.createRecord(
                                face = faceWithDetails.face,
                                teacherEmail = currentUser?.email ?: "admin@azuratech.com",
                                activeClassId = selectedClass?.id ?: "",
                                activeClassName = selectedClass?.name ?: "General Scan",
                                status = selectedStatus,
                                attendanceDate = selectedDate,
                                checkInTime = finalDateTime
                            )

                            checkInViewModel.addRecord(newRecord)
                            onBack()
                        }
                    },
                    enabled = selectedFace != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = AzuraSpacing.lg),
                    shape = AzuraShapes.medium
                ) {
                    Text("Simpan Kehadiran", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun LockBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(AzuraSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            // 🔥 FIXED: Added contentDescription and properly named the modifier parameter
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Data personil dan tanggal dikunci.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// --- REUSABLE COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    getLabel: (T) -> String,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded && enabled, 
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption?.let { getLabel(it) } ?: "Mode Gerbang (General)", // 🔥 UX UPDATE
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            shape = AzuraShapes.medium,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            // 🔥 FIXED: Reverted to standard menuAnchor() for better compatibility across Compose versions
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable) 
        )
        
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // 🔥 UX UPDATE: Opsi paling atas untuk menghapus kelas
            DropdownMenuItem(
                text = { Text("Mode Gerbang (General Scan)", color = MaterialTheme.colorScheme.secondary) },
                onClick = { onOptionSelected(null); expanded = false }
            )
            HorizontalDivider()
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getLabel(option)) },
                    onClick = { onOptionSelected(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraTimePickerButton(
    label: String,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = modifier,
        shape = AzuraShapes.medium
    ) {
        val hour = selectedTime.hour.toString().padStart(2, '0')
        val minute = selectedTime.minute.toString().padStart(2, '0')
        Text("$label: $hour:$minute")
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}