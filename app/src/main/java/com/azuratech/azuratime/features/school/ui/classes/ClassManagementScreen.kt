package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.azuratech.azuratime.core.ui.util.UiState
import com.azuratech.azuraengine.model.ClassModel

@Composable
fun ClassManagementScreen(
    onClassSelected: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ClassViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    val availableClasses by viewModel.availableClassesStateFlow.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    AzuraScreen(
        title = "Manajemen Kelas",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        }
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
                ) {
                    items(state.data) { classModel ->
                        ClassItem(
                            classModel = classModel,
                            onClick = { onClassSelected(classModel.id, classModel.name) }
                        )
                    }
                }
            }
            is UiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada kelas yang terdaftar.")
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message ?: "Terjadi kesalahan", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showAddDialog) {
        AddClassDialog(
            availableClasses = availableClasses,
            onDismissRequest = { showAddDialog = false },
            onConfirmClick = { name ->
                viewModel.createClass(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ClassItem(classModel: ClassModel, onClick: () -> Unit) {
    AzuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.sm), // Inner padding because AzuraCard already has padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column {
                Text(text = classModel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${classModel.studentCount} Siswa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
