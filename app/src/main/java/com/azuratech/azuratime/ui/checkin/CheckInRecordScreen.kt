package com.azuratech.azuratime.ui.checkin

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.User
import com.azuratech.azuratime.ui.CheckInRecordEntityCard
import com.azuratech.azuratime.ui.core.designsystem.AttendanceActionSheet
import com.azuratech.azuratime.ui.core.designsystem.AzuraDatePickerButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraDropdownField
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.*
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.user.UserManagementViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInRecordScreen(
    userEmail: String,
    onNavigateBack: () -> Unit = {},
    checkInViewModel: CheckInViewModel,
    userViewModel: UserManagementViewModel,
    classViewModel: ClassViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 1. Observation
    val globalClasses by classViewModel.classes.collectAsStateWithLifecycle(emptyList())
    val user by userViewModel.currentUser.collectAsStateWithLifecycle()
    val records by checkInViewModel.checkInRecords.collectAsStateWithLifecycle()
    val filterParams by checkInViewModel.filterParams.collectAsStateWithLifecycle()
    val assignedIds by userViewModel.assignedClassIds.collectAsStateWithLifecycle(emptyList())

    var editingRecord by remember { mutableStateOf<CheckInRecordEntity?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var showClassCorrectionDialog by remember { mutableStateOf<CheckInRecordEntity?>(null) }

    // 2. Filter Sync
    LaunchedEffect(user, startDate, endDate, selectedClassId) {
        @Suppress("UNUSED_VARIABLE") val filterUserId = if (user?.role == "SUPER_ADMIN" || user?.membershipRole == "ADMIN") null else userEmail
        checkInViewModel.updateFilters(
            start = startDate,
            end = endDate
        )
    }

    val availableClasses = remember(globalClasses, assignedIds) {
        if (user?.role == "SUPER_ADMIN" || user?.membershipRole == "ADMIN") globalClasses else globalClasses.filter { it.id in assignedIds }
    }

    AzuraScreen(
        title = "History Log (${records.size})", 
        onBack = onNavigateBack
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = AzuraSpacing.md)) {
            
            // --- HEADER ACTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = AzuraSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterToggleButton(
                    isActive = showFilters, 
                    onClick = { showFilters = !showFilters }
                )

                Button(
                    onClick = { checkInViewModel.exportRecords(records) },
                    shape = AzuraShapes.medium,
                    enabled = records.isNotEmpty()
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = filterParams.name,
                onValueChange = { checkInViewModel.updateNameFilter(it) },
                placeholder = { Text("Cari nama siswa...") },
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (filterParams.name.isNotEmpty()) {
                        IconButton(onClick = { checkInViewModel.updateNameFilter("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )

            // --- FILTER PANEL ---
            if (showFilters) {
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                LocalFilterPanel(
                    startDate = startDate,
                    endDate = endDate,
                    classes = availableClasses,
                    selectedClassId = selectedClassId,
                    onDatesChanged = { s, e -> startDate = s; endDate = e },
                    onClassSelected = { selectedClassId = it }
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
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        CheckInRecordEntityCard(
                            record = record,
                            onEditRequested = { editingRecord = it }
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
                    checkInViewModel.deleteRecord(record)
                    editingRecord = null 
                },
                onUpdateStatus = { record -> 
                    checkInViewModel.updateRecord(record)
                    editingRecord = null 
                },
                onShowClassCorrection = { 
                    showClassCorrectionDialog = selectedRecord
                    editingRecord = null 
                }
            )
        }

        showClassCorrectionDialog?.let { recordToCorrect ->
            LocalClassCorrectionDialog(
                currentClassName = recordToCorrect.className ?: "General Scan",
                userClasses = availableClasses,
                onDismiss = { showClassCorrectionDialog = null },
                onClassSelected = { classItem ->
                    checkInViewModel.updateRecordClass(recordToCorrect, classItem)
                    showClassCorrectionDialog = null
                }
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
    onClassSelected: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                getOptionLabel = { it.name }
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
    onClassSelected: (ClassModel) -> Unit
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
                            shape = AzuraShapes.medium
                        ) { Text(classItem.name) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}