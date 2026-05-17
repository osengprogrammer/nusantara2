package com.azuratech.azuratime.features.attendance.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.ui.designsystem.AttendanceActionSheet
import com.azuratech.azuratime.core.ui.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraDropdownField
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.*
import com.azuratech.azuratime.features.school.ui.classes.ClassViewModel
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    userEmail: String,
    onNavigateBack: () -> Unit = {},
    attendanceViewModel: AttendanceViewModel,
    userViewModel: AccountManagementViewModel,
    classViewModel: ClassViewModel,
) {
    // 1. Observation
    val globalClasses by classViewModel.classesStateFlow.collectAsStateWithLifecycle()
    val user by userViewModel.currentUser.collectAsStateWithLifecycle()
    val records by attendanceViewModel.attendanceRecords.collectAsStateWithLifecycle()
    val filterParams by attendanceViewModel.filterParams.collectAsStateWithLifecycle()
    val assignedIds by userViewModel.assignedClassIds.collectAsStateWithLifecycle()

    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var showClassCorrectionDialog by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Role helper
    val activeSchoolId = user?.activeSchoolId ?: ""
    val userRole = user?.memberships?.get(activeSchoolId)?.role ?: user?.role ?: "USER"
    val isAdmin = userRole == "ADMIN" || userRole == "SUPER_ADMIN"

    // 2. Filter Sync
    LaunchedEffect(user, startDate, endDate, selectedClassId) {
        attendanceViewModel.updateFilters(
            start = startDate,
            end = endDate,
        )
    }

    val availableClasses = remember(globalClasses, assignedIds, isAdmin) {
        if (isAdmin) globalClasses else globalClasses.filter { it.id in assignedIds }
    }

    AzuraScreen(
        title = "History Log (${records.size})",
        onBack = onNavigateBack,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = AzuraSpacing.md)) {
            // --- HEADER ACTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = AzuraSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterToggleButton(
                    isActive = showFilters,
                    onClick = { showFilters = !showFilters },
                )

                Button(
                    onClick = { attendanceViewModel.exportRecords(records) },
                    shape = AzuraShapes.medium,
                    enabled = records.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = filterParams.name,
                onValueChange = { attendanceViewModel.updateNameFilter(it) },
                placeholder = { Text("Cari nama siswa...") },
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (filterParams.name.isNotEmpty()) {
                        IconButton(onClick = { attendanceViewModel.updateNameFilter("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
            )

            // --- FILTER PANEL ---
            if (showFilters) {
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                LocalFilterPanel(
                    startDate = startDate,
                    endDate = endDate,
                    classes = availableClasses,
                    selectedClassId = selectedClassId,
                    onDatesChanged = { s, e ->
                        startDate = s
                        endDate = e
                    },
                    onClassSelected = { selectedClassId = it },
                )
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            // --- RECORDS LIST ---
            if (records.isEmpty()) {
                LocalEmptyPlaceholder("Tidak ada log ditemukan.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(records, key = { it.recordId }) { record ->
                        AttendanceHistoryCard(
                            record = record,
                            onEditRequested = { editingRecord = record },
                        )
                    }
                }
            }
        }

        // --- DIALOGS ---
        editingRecord?.let { selectedRecord ->
            AttendanceActionSheet(
                record = selectedRecord,
                onDismiss = { editingRecord = null },
                onDelete = { record ->
                    attendanceViewModel.deleteRecord(record)
                    editingRecord = null
                },
                onUpdateStatus = { record, status ->
                    attendanceViewModel.updateRecordStatus(record, status)
                    editingRecord = null
                },
                onShowClassCorrection = {
                    showClassCorrectionDialog = selectedRecord
                    editingRecord = null
                },
            )
        }

        showClassCorrectionDialog?.let { recordToCorrect ->
            LocalClassCorrectionDialog(
                currentClassName = recordToCorrect.className.ifBlank { "General Scan" },
                userClasses = availableClasses,
                onDismiss = { showClassCorrectionDialog = null },
                onClassSelected = { classItem ->
                    attendanceViewModel.updateRecordClass(recordToCorrect, classItem)
                    showClassCorrectionDialog = null
                },
            )
        }
    }
}

// --- LOCAL HELPERS TO FIX UNRESOLVED REFERENCES ---

@Composable
fun FilterToggleButton(isActive: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = AzuraShapes.medium) {
        Icon(if (isActive) Icons.Default.FilterListOff else Icons.Default.FilterList, null)
        Spacer(Modifier.width(8.dp))
        Text(if (isActive) "Tutup Filter" else "Filter Data")
    }
}

@Composable
fun LocalFilterPanel(
    startDate: LocalDate?,
    endDate: LocalDate?,
    classes: List<ClassModel>,
    selectedClassId: String?,
    onDatesChanged: (LocalDate?, LocalDate?) -> Unit,
    onClassSelected: (String?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        var isClassExpanded by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(AzuraSpacing.md), verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                AzuraDatePickerButton("Dari", startDate, { onDatesChanged(it, endDate) }, Modifier.weight(1f))
                AzuraDatePickerButton("Sampai", endDate, { onDatesChanged(startDate, it) }, Modifier.weight(1f))
            }

            AzuraDropdownField(
                label = "Filter Kelas",
                selectedValue = classes.find { it.id == selectedClassId }?.name ?: "Semua Kelas",
                options = classes,
                isExpanded = isClassExpanded,
                onExpandedChange = { isClassExpanded = it },
                onOptionSelected = { onClassSelected(it.id) },
                onEditClicked = {},
                getOptionLabel = { it.name },
            )

            if (selectedClassId != null) {
                TextButton(onClick = { onClassSelected(null) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Reset Kelas", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun LocalEmptyPlaceholder(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
            Text(msg, color = Color.Gray)
        }
    }
}

@Composable
fun LocalClassCorrectionDialog(
    currentClassName: String,
    userClasses: List<ClassModel>,
    onDismiss: () -> Unit,
    onClassSelected: (ClassModel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindahkan ke Sesi Kelas") },
        text = {
            Column {
                Text("Sesi saat ini: $currentClassName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(userClasses) { classItem ->
                        OutlinedButton(
                            onClick = { onClassSelected(classItem) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = AzuraShapes.medium,
                        ) { Text(classItem.name) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
