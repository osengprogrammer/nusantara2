package com.azuratech.azuratime.features.reporting.ui.integrity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DataIntegrityScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataIntegrityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is DataIntegrityUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AzuraScreen(
        title = "System Health",
        onBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            item {
                IntegritySummarySection(uiState)
            }

            if (uiState.conflicts.isNotEmpty()) {
                item {
                    Text("Attendance Conflicts", style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.conflicts) { conflict ->
                    ConflictItem(
                        conflict = conflict,
                        onResolve = { useCloud ->
                            viewModel.onEvent(DataIntegrityUiEvent.ResolveConflict(conflict.conflictId, useCloud))
                        },
                    )
                }
            } else if (!uiState.isLoading) {
                item {
                    AzuraCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(AzuraSpacing.xl), contentAlignment = Alignment.Center) {
                            Text("No data conflicts detected.")
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun IntegritySummarySection(state: DataIntegrityUiState) {
    AzuraCard(title = "System Summary") {
        Column(modifier = Modifier.padding(AzuraSpacing.md), verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
            IntegrityRow("Total Students", state.totalStudents.toString())
            IntegrityRow("Total Attendance", state.totalRecords.toString())
            IntegrityRow("Students Without Class", state.missingAssignments.toString(), isError = state.missingAssignments > 0)
            IntegrityRow("Unsynced Data", state.unsyncedCount.toString(), isWarning = state.unsyncedCount > 0)
        }
    }
}

@Composable
fun IntegrityRow(label: String, value: String, isError: Boolean = false, isWarning: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                isError -> MaterialTheme.colorScheme.error
                isWarning -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
fun ConflictItem(conflict: AttendanceConflict, onResolve: (Boolean) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM, HH:mm") }
    val dateTime = remember(conflict.local.timestamp) {
        Instant.ofEpochMilli(conflict.local.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(text = "Conflict: ${conflict.local.studentName}", style = MaterialTheme.typography.labelLarge)
            Text(text = "Time: ${dateTime.format(formatter)}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                Button(onClick = { onResolve(true) }, modifier = Modifier.weight(1f)) {
                    Text("Use Cloud (${conflict.cloud.status})")
                }
                OutlinedButton(onClick = { onResolve(false) }, modifier = Modifier.weight(1f)) {
                    Text("Use Local (${conflict.local.status})")
                }
            }
        }
    }
}
