package com.azuratech.azuratime.features.school.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraLoadingButton
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme

@Composable
fun SchoolListScreen(
    viewModel: SchoolViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") onSchoolClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    AzuraScreen(
        title = "Manajemen Sekolah",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Sekolah")
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
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(AzuraSpacing.md))
                        AzuraLoadingButton(
                            text = "Retry",
                            isLoading = false,
                            onClick = { viewModel.onEvent(SchoolUiEvent.Retry) },
                        )
                    }
                }
                uiState.schools.isEmpty() -> {
                    Text(
                        text = "Belum ada sekolah.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    // Placeholder for actual list rendering logic
                    // In a full implementation, you'd iterate over uiState.schools and render cards
                    Text(
                        text = "Terdapat ${uiState.schools.size} sekolah terdaftar.",
                        modifier = Modifier.padding(AzuraSpacing.md),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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
                text = "Terdapat ${mockState.schools.size} sekolah terdaftar.",
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
            AzuraLoadingButton(text = "Retry", isLoading = false, onClick = {})
        }
    }
}
