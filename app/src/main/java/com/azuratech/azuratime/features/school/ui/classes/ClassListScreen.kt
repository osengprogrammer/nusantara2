package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing

@Composable
fun ClassListScreen(
    onNavigateToDetail: (classId: String, className: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ClassViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is ClassUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ClassUiEffect.NavigateTo -> {} // Handle if needed
            }
        }
    }

    AzuraScreen(
        title = "Class Management",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(
                onClick = { viewModel.onEvent(ClassUiEvent.SyncClasses) },
                enabled = !uiState.isLoading,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ClassUiEvent.ShowAddDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = AzuraShapes.medium,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
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
                        TextButton(onClick = { viewModel.onEvent(ClassUiEvent.ClearError) }) {
                            Text("OK")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading && uiState.classes.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.classes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No classes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        contentPadding = PaddingValues(top = AzuraSpacing.md, bottom = 100.dp),
                    ) {
                        items(uiState.classes, key = { it.id }) { classItem ->
                            ClassItemCard(
                                classItem = classItem,
                                onClick = { onNavigateToDetail(classItem.id, classItem.name) },
                                onEditClick = { viewModel.onEvent(ClassUiEvent.RequestEditClass(classItem)) },
                                onDeleteClick = { viewModel.onEvent(ClassUiEvent.RequestDeleteClass(classItem)) },
                            )
                        }
                    }
                }
            }
        }

        // --- ➕ DIALOG ADD ---
        if (uiState.isAddDialogVisible) {
            AddClassDialog(
                availableClasses = uiState.availableClasses,
                onDismissRequest = { viewModel.onEvent(ClassUiEvent.DismissAddDialog) },
                onConfirmClick = { newName ->
                    viewModel.onEvent(ClassUiEvent.CreateClass(newName))
                },
            )
        }

        // --- ✏️ DIALOG EDIT ---
        uiState.classToEdit?.let { item ->
            AddClassDialog(
                editingClass = item,
                availableClasses = uiState.availableClasses,
                onDismissRequest = { viewModel.onEvent(ClassUiEvent.CancelEditClass) },
                onConfirmClick = { newName ->
                    viewModel.onEvent(ClassUiEvent.UpdateClass(item.id, newName))
                },
            )
        }

        // --- 🗑️ DIALOG DELETE ---
        uiState.classToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ClassUiEvent.CancelDeleteClass) },
                icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Delete Class?") },
                text = { Text("Deleting '${item.name}' will disconnect students from this class.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onEvent(ClassUiEvent.ConfirmDeleteClass) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(ClassUiEvent.CancelDeleteClass) }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
fun ClassItemCard(
    classItem: ClassModel,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(AzuraSpacing.md))

            Text(
                text = classItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
