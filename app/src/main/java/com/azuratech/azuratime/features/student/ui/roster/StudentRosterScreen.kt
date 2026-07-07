package com.azuratech.azuratime.features.student.ui.roster

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.student.ui.components.StudentRosterRow
import androidx.compose.ui.platform.LocalContext
import com.azuratech.azuratime.core.util.showToast

/**
 * 🎓 STUDENT ROSTER SCREEN (v3.2.0-ai-native)
 * Unified management screen for student profiles.
 */
@Composable
fun StudentRosterScreen(
    onEditStudentClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToWallet: (String) -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToDeduct: (String) -> Unit,
    viewModel: StudentRosterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is StudentRosterUiEffect.ShowToast -> context.showToast(effect.message)
                is StudentRosterUiEffect.NavigateToDetail -> {
                    onEditStudentClick(effect.studentId)
                }
                is StudentRosterUiEffect.ExportPdf -> { /* Only handled in Barcode Screen */ }
            }
        }
    }

    AzuraScreen(
        title = "Student Roster",
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
                    onValueChange = { viewModel.onEvent(StudentRosterUiEvent.UpdateSearch(it)) },
                    placeholder = { Text("Search student name or ID...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzuraShapes.medium,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(StudentRosterUiEvent.UpdateSearch("")) }) {
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

            // --- 2. ACTION ROW (Sync, Filter Summary) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 🔥 Sync Roster Button
                Surface(
                    onClick = { viewModel.onEvent(StudentRosterUiEvent.SyncStudents) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Roster", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                // Info Badge
                Surface(
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(40.dp),
                ) {
                    Box(modifier = Modifier.padding(horizontal = AzuraSpacing.md), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${uiState.students.size} Students",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(AzuraSpacing.sm))

            // --- 3. CLASS FILTER (Horizontal Chips) ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AzuraSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedClassId == null,
                        onClick = { viewModel.onEvent(StudentRosterUiEvent.SelectClass(null)) },
                        label = { Text("All Classes") },
                        shape = AzuraShapes.medium,
                    )
                }
                items(uiState.allClasses) { classModel ->
                    FilterChip(
                        selected = uiState.selectedClassId == classModel.id,
                        onClick = { viewModel.onEvent(StudentRosterUiEvent.SelectClass(classModel.id)) },
                        label = { Text(classModel.name) },
                        shape = AzuraShapes.medium,
                    )
                }
            }

            Spacer(Modifier.height(AzuraSpacing.sm))

            // Student List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    items(uiState.students, key = { it.studentId }) { item ->
    StudentRosterRow(
        studentId = item.studentId, // <-- Ensure this is passed!
        name = item.displayName,
        code = item.studentCode,
        classNames = item.assignedClassNames,
        balance = item.formattedBalance(),
        isBiometricReady = item.isBiometricReady,
        onClick = { onNavigateToWallet(item.studentId) }, // Or your existing navigation logic
        onHistoryClick = onNavigateToHistory,
        onDeductClick = onNavigateToDeduct
    )
}

                    if (uiState.students.isEmpty() && !uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AzuraSpacing.xl),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No students found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading && uiState.students.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(StudentRosterUiEvent.CancelDelete) },
            title = { Text("Delete Student?") },
            text = { Text("Student data will be permanently deleted from device and cloud.") },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.targetStudentId?.let {
                            viewModel.onEvent(StudentRosterUiEvent.ConfirmDelete(it))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(StudentRosterUiEvent.CancelDelete) }) {
                    Text("Cancel")
                }
            },
        )
    }
}
