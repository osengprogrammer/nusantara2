package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuraengine.model.ClassModel

@Composable
fun ClassManagementScreen(
    onClassSelected: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ClassViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    AzuraScreen(
        title = "Manajemen Kelas",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(
                onClick = { viewModel.onEvent(ClassUiEvent.SyncClasses) },
                enabled = !uiState.isLoading,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sinkronkan Kelas")
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(ClassUiEvent.ShowAddDialog) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.classes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.classes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada kelas yang terdaftar.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    items(uiState.classes) { classModel ->
                        ClassItem(
                            classModel = classModel,
                            onClick = { onClassSelected(classModel.id, classModel.name) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddDialogVisible) {
        AddClassDialog(
            availableClasses = uiState.availableClasses,
            onDismissRequest = { viewModel.onEvent(ClassUiEvent.DismissAddDialog) },
            onConfirmClick = { name ->
                viewModel.onEvent(ClassUiEvent.CreateClass(name))
            },
        )
    }
}

@Composable
fun ClassItem(classModel: ClassModel, onClick: () -> Unit) {
    AzuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column {
                Text(text = classModel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${classModel.studentCount} Siswa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
