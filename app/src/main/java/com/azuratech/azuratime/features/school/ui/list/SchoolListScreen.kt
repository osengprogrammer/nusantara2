package com.azuratech.azuratime.features.school.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraLoadingButton
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.designsystem.theme.AzuraTheme
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.domain.model.AccountRole

@Composable
fun SchoolListScreen(
    viewModel: SchoolViewModel = hiltViewModel(),
    onSchoolClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchoolId by remember { mutableStateOf<String?>(null) }
    var initialSchoolName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is SchoolUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is SchoolUiEffect.NavigateTo -> {} // Handle if needed
            }
        }
    }

    AzuraScreen(
        title = stringResource(R.string.label_organization_plural),
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            if (uiState.currentAccountRole == AccountRole.ADMIN || uiState.currentAccountRole == AccountRole.SUPER_ADMIN) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_org))
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(AzuraSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.error_unknown),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(AzuraSpacing.md))
                        AzuraLoadingButton(
                            text = stringResource(R.string.action_retry),
                            isLoading = false,
                            onClick = { viewModel.onEvent(SchoolUiEvent.Retry) },
                        )
                    }
                }
                uiState.schools.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.empty_sessions_registered),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AzuraSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    ) {
                        items(uiState.schools, key = { it.id }) { school ->
                            val isActive = school.id == uiState.activeSchoolId

                            Card(
                                onClick = {
                                    android.util.Log.d("SchoolClick", "Clicked school: ${school.id}, name: ${school.name}")
                                    viewModel.onEvent(SchoolUiEvent.SelectSchool(school))
                                    onSchoolClick(school.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AzuraShapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                                border = if (isActive) {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(AzuraSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        color = if (school.status == "ACTIVE") {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = AzuraShapes.small,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.School,
                                                contentDescription = null,
                                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(AzuraSpacing.md))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = school.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = stringResource(R.string.label_status).format(school.status),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (school.status == "ACTIVE") {
                                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                            } else {
                                                androidx.compose.ui.graphics.Color.Gray
                                            },
                                        )
                                    }

                                    IconButton(onClick = {
                                        initialSchoolName = school.name
                                        editingSchoolId = school.id
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.action_edit),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        )
                                    }

                                    if (isActive) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = stringResource(R.string.label_status_active),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    } else {
                                        IconButton(onClick = { viewModel.onEvent(SchoolUiEvent.DeleteSchool(school.id)) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.action_delete),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSchoolDialog(
            availableClasses = uiState.availableClasses,
            onDismissRequest = { showAddDialog = false },
            onConfirmClick = { name, timezone, selectedClassIds ->
                viewModel.onEvent(SchoolUiEvent.CreateSchool(name, timezone, selectedClassIds))
                showAddDialog = false
            },
        )
    }

    editingSchoolId?.let { schoolId ->
        var newName by remember { mutableStateOf(initialSchoolName) }
        AlertDialog(
            onDismissRequest = { editingSchoolId = null },
            title = { Text(stringResource(R.string.dialog_edit_org_name)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.label_organization_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.onEvent(SchoolUiEvent.UpdateSchoolName(schoolId, newName))
                            editingSchoolId = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSchoolId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoading() {
    AzuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSuccess() {
    AzuraTheme {
        val mockState = SchoolPreviewMocks.success()
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "There are ${mockState.schools.size} schools registered.",
                modifier = Modifier.padding(AzuraSpacing.md),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewError() {
    AzuraTheme {
        val mockState = SchoolPreviewMocks.error()
        Column(
            modifier = Modifier.fillMaxSize().padding(AzuraSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mockState.error ?: "",
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(AzuraSpacing.md))
            AzuraLoadingButton(text = stringResource(R.string.action_retry), isLoading = false, onClick = {})
        }
    }
}
