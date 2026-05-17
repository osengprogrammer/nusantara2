package com.azuratech.azuratime.features.attendance.ui

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
import com.azuratech.azuratime.core.ui.designsystem.AzuraDropdownField
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.*
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.attendance.ui.history.AttendanceHistoryCard

/**
 * 📝 ATTENDANCE SCREEN (v3.2.0-ai-native)
 * Main management screen for viewing and correcting attendance logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AttendanceViewModel,
    accountViewModel: AccountManagementViewModel,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val user by accountViewModel.currentAccountFlow.collectAsStateWithLifecycle()
    val assignedIds by accountViewModel.assignedClassIdsFlow.collectAsStateWithLifecycle()

    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showClassCorrectionDialog by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Role helper
    val accountRole = user?.memberships?.get(user?.activeSchoolId)?.role ?: user?.role ?: "MEMBER"
    val isAdmin = accountRole == "ADMIN" || accountRole == "SUPER_ADMIN"

    val availableClasses = remember(uiState.classes, assignedIds, isAdmin) {
        if (isAdmin) uiState.classes else uiState.classes.filter { it.id in assignedIds }
    }

    AzuraScreen(
        title = "History Log (${uiState.records.size})",
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
                    onClick = { viewModel.onEvent(AttendanceUiEvent.ExportRecords(uiState.records)) },
                    shape = AzuraShapes.medium,
                    enabled = uiState.records.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV")
                }
            }

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(AttendanceUiEvent.UpdateSearchQuery(it)) },
                placeholder = { Text("Cari nama siswa...") },
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(AttendanceUiEvent.UpdateSearchQuery("")) }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
            )

            // --- FILTER PANEL ---
            if (showFilters) {
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                LocalFilterPanel(
                    classes = availableClasses,
                    selectedClassId = uiState.selectedClassId,
                    onClassSelected = { viewModel.onEvent(AttendanceUiEvent.SelectClass(it)) },
                )
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            // --- RECORDS LIST ---
            if (uiState.records.isEmpty() && !uiState.isLoading) {
                LocalEmptyPlaceholder("Tidak ada log ditemukan.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(uiState.records, key = { it.recordId }) { record ->
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
                    viewModel.onEvent(AttendanceUiEvent.DeleteRecord(record))
                    editingRecord = null
                },
                onUpdateStatus = { record, status ->
                    viewModel.onEvent(AttendanceUiEvent.UpdateRecordStatus(record, status))
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
                    viewModel.onEvent(AttendanceUiEvent.UpdateRecordClass(recordToCorrect, classItem))
                    showClassCorrectionDialog = null
                },
            )
        }
    }
}

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
    classes: List<ClassModel>,
    selectedClassId: String?,
    onClassSelected: (String?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        var isClassExpanded by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(AzuraSpacing.md), verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewLoading() {
    AzuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewSuccess() {
    AzuraTheme {
        val mockState = AttendancePreviewMocks.success()
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            mockState.records.forEach { record ->
                AttendanceHistoryCard(record = record)
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewError() {
    AzuraTheme {
        val mockState = AttendancePreviewMocks.error()
        Column(
            modifier = Modifier.fillMaxSize().padding(AzuraSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = mockState.error ?: "Error", color = MaterialTheme.colorScheme.error)
        }
    }
}
