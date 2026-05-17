package com.azuratech.azuratime.features.biometric.ui.assignment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme

@Composable
fun StudentAssignmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentAssignmentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Penugasan Kelas",
        onBack = onNavigateBack,
        actions = {
            IconButton(onClick = { viewModel.onEvent(AssignmentUiEvent.Refresh) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.roster.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.roster.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.onEvent(AssignmentUiEvent.Refresh) }) {
                        Text("Coba Lagi")
                    }
                }
            } else {
                StudentRosterList(
                    roster = uiState.roster,
                    assignedClasses = uiState.assignedClasses,
                    availableClasses = uiState.availableClasses,
                    onAssign = { sId, cId -> viewModel.onEvent(AssignmentUiEvent.AssignStudent(sId, cId)) },
                    onRemove = { sId, cId -> viewModel.onEvent(AssignmentUiEvent.RemoveAssignment(sId, cId)) },
                )
            }

            if (uiState.isLoading && uiState.roster.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun StudentRosterList(
    roster: List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>,
    assignedClasses: Map<String, List<com.azuratech.azuraengine.model.ClassModel>>,
    availableClasses: List<com.azuratech.azuraengine.model.ClassModel>,
    onAssign: (String, String) -> Unit,
    onRemove: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AzuraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
    ) {
        items(roster, key = { it.biometric.studentId }) { details ->
            val studentId = details.biometric.studentId
            val currentAssigned = assignedClasses[studentId] ?: emptyList()

            AzuraCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudentAvatar(photoPath = details.biometric.photoUrl, size = 48.dp)
                        Spacer(modifier = Modifier.width(AzuraSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(details.biometric.name, style = MaterialTheme.typography.titleMedium)
                            Text("ID: $studentId", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                    Text("Kelas Terdaftar:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        currentAssigned.forEach { cls ->
                            InputChip(
                                selected = true,
                                onClick = { onRemove(studentId, cls.id) },
                                label = { Text(cls.name, style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                            )
                        }

                        // Add class button
                        var showClassPicker by remember { mutableStateOf(false) }
                        AssistChip(
                            onClick = { showClassPicker = true },
                            label = { Text("Tambah", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) },
                        )

                        if (showClassPicker) {
                            ClassPicker(
                                availableClasses = availableClasses.filter { cls -> currentAssigned.none { it.id == cls.id } },
                                onDismiss = { showClassPicker = false },
                                onSelected = {
                                    onAssign(studentId, it.id)
                                    showClassPicker = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() },
    )
}

@Composable
fun ClassPicker(
    availableClasses: List<com.azuratech.azuraengine.model.ClassModel>,
    onDismiss: () -> Unit,
    onSelected: (com.azuratech.azuraengine.model.ClassModel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Kelas") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(availableClasses) { cls ->
                    TextButton(
                        onClick = { onSelected(cls) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(cls.name)
                    }
                }
                if (availableClasses.isEmpty()) {
                    item { Text("Tidak ada kelas tersedia", color = Color.Gray) }
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
        val mockState = AssignmentPreviewMocks.success()
        StudentRosterList(
            roster = mockState.roster,
            assignedClasses = mockState.assignedClasses,
            availableClasses = mockState.availableClasses,
            onAssign = { _, _ -> },
            onRemove = { _, _ -> },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewError() {
    AzuraTheme {
        val mockState = AssignmentPreviewMocks.error()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mockState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                Button(onClick = { }) { Text("Retry") }
            }
        }
    }
}
