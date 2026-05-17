package com.azuratech.azuratime.features.student.ui.roster

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.student.ui.components.StudentRosterItem

@Composable
fun StudentRosterScreen(
    onEditStudentClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: StudentRosterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val rotation = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    AzuraScreen(
        title = "Roster Siswa",
        onBack = onNavigateBack,
        actions = {
            IconButton(
                onClick = { viewModel.onEvent(StudentRosterUiEvent.SyncStudents) },
                enabled = !uiState.isLoading,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Sync",
                    modifier = if (uiState.isLoading) Modifier.rotate(rotation.value) else Modifier,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Error Banner
            uiState.error?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AzuraSpacing.md),
                ) {
                    Row(
                        modifier = Modifier.padding(AzuraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = errorMsg,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = { viewModel.onEvent(StudentRosterUiEvent.ClearError) }) {
                            Text("OK")
                        }
                    }
                }
            }

            // Search Bar
            AzuraTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(StudentRosterUiEvent.UpdateSearch(it)) },
                label = "Cari Siswa / ID",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
            )

            // Class Filter
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AzuraSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedClassId == null,
                        onClick = { viewModel.onEvent(StudentRosterUiEvent.SelectClass(null)) },
                        label = { Text("Semua") },
                    )
                }
                items(uiState.allClasses) { classModel ->
                    FilterChip(
                        selected = uiState.selectedClassId == classModel.id,
                        onClick = { viewModel.onEvent(StudentRosterUiEvent.SelectClass(classModel.id)) },
                        label = { Text(classModel.name) },
                    )
                }
            }

            // Student List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    items(uiState.students, key = { it.profile.studentId }) { item ->
                        StudentRosterItem(
                            item = item,
                            onEditClick = { onEditStudentClick(item.profile.studentId) },
                            onDeleteClick = { viewModel.onEvent(StudentRosterUiEvent.RequestDelete(item.profile.studentId)) },
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
                                    "Tidak ada siswa ditemukan",
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
            title = { Text("Hapus Siswa?") },
            text = { Text("Data siswa akan dihapus secara permanen dari perangkat dan cloud.") },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.targetStudentId?.let {
                            viewModel.onEvent(StudentRosterUiEvent.ConfirmDelete(it))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(StudentRosterUiEvent.CancelDelete) }) {
                    Text("Batal")
                }
            },
        )
    }
}
