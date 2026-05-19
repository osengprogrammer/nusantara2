package com.azuratech.azuratime.features.attendance.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.ui.designsystem.AttendanceActionSheet
import com.azuratech.azuratime.core.ui.designsystem.AzuraDropdownField
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.*
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.ui.history.AttendanceHistoryCard
import com.azuratech.azuratime.core.util.showToast

/**
 * 📝 ATTENDANCE SCREEN (v3.2.0-ai-native)
 * Main management screen for viewing and correcting attendance logs.
 * BEAUTIFIED & RESPONSIVE (v3.7.1)
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
    val context = LocalContext.current

    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showClassCorrectionDialog by remember { mutableStateOf<AttendanceRecord?>(null) }

    // 🔥 AI Native: Toast Feedback
    LaunchedEffect(uiState.exportPath) {
        uiState.exportPath?.let {
            context.showToast("Berhasil diekspor: $it")
            viewModel.onEvent(AttendanceUiEvent.ClearError) // Clear the state if needed, or handle path reset
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            context.showToast("Gagal: $it")
        }
    }

    // Role helper
    val accountRole = user?.memberships?.get(user?.activeSchoolId)?.role ?: user?.role ?: "MEMBER"
    val isAdmin = accountRole == "ADMIN" || accountRole == "SUPER_ADMIN"

    val availableClasses = remember(uiState.classes, assignedIds, isAdmin) {
        if (isAdmin) uiState.classes else uiState.classes.filter { it.id in assignedIds }
    }

    AzuraScreen(
        title = "History Log",
        onBack = onNavigateBack,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. SLEEK SEARCH BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onEvent(AttendanceUiEvent.UpdateSearchQuery(it)) },
                    placeholder = { Text("Cari nama siswa...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzuraShapes.medium,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(AttendanceUiEvent.UpdateSearchQuery("")) }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                    singleLine = true,
                )
            }

            // --- 2. ACTION ROW (Sync, CSV, Filter Toggle) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 🔥 Sync History Button (Responsive)
                Surface(
                    onClick = { viewModel.onEvent(AttendanceUiEvent.SyncHistory) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSyncing,
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            Icon(Icons.Default.Sync, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                        Text(
                            text = "Sync",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // 📥 Export CSV Button
                Surface(
                    onClick = { viewModel.onEvent(AttendanceUiEvent.ExportRecords(uiState.records)) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.records.isNotEmpty() && !uiState.isExporting,
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        } else {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                        Text(
                            text = if (uiState.isExporting) "Export..." else "CSV",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // 🛠️ Filter Toggle
                FilledIconButton(
                    onClick = { showFilters = !showFilters },
                    shape = AzuraShapes.medium,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showFilters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList, null)
                }
            }

            // --- 3. FILTER PANEL (Animated) ---
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                LocalFilterPanel(
                    classes = availableClasses,
                    selectedClassId = uiState.selectedClassId,
                    onClassSelected = { viewModel.onEvent(AttendanceUiEvent.SelectClass(it)) },
                )
            }

            // --- 4. STATUS SUMMARY ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Menampilkan ${uiState.records.size} log",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (uiState.selectedClassId != null) {
                    Spacer(Modifier.width(AzuraSpacing.sm))
                    val selectedClassName = availableClasses.find { it.id == uiState.selectedClassId }?.name ?: "Kelas"
                    AssistChip(
                        onClick = { viewModel.onEvent(AttendanceUiEvent.SelectClass(null)) },
                        label = { Text(selectedClassName) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                        shape = AzuraShapes.small,
                    )
                }
            }

            // --- 5. RECORDS LIST ---
            if (uiState.records.isEmpty() && !uiState.isLoading) {
                LocalEmptyPlaceholder("Tidak ada log ditemukan.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(
                        start = AzuraSpacing.md,
                        end = AzuraSpacing.md,
                        bottom = 100.dp,
                    ),
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
fun LocalFilterPanel(
    classes: List<ClassModel>,
    selectedClassId: String?,
    onClassSelected: (String?) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.xs)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), AzuraShapes.medium)
            .padding(AzuraSpacing.md),
    ) {
        var isClassExpanded by remember { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
            AzuraDropdownField(
                label = "Filter per Kelas",
                selectedValue = classes.find { it.id == selectedClassId }?.name ?: "Semua Kelas",
                options = classes,
                isExpanded = isClassExpanded,
                onExpandedChange = { isClassExpanded = it },
                onOptionSelected = { onClassSelected(it.id) },
                onEditClicked = {},
                getOptionLabel = { it.name },
            )

            if (selectedClassId != null) {
                TextButton(
                    onClick = { onClassSelected(null) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Filter", style = MaterialTheme.typography.labelMedium)
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
