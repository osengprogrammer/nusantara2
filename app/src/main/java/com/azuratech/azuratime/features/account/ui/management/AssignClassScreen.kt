package com.azuratech.azuratime.features.account.ui.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignClassScreen(
    targetAccountId: String,
    onNavigateBack: () -> Unit,
    viewModel: AssignClassViewModel,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onEvent(AssignClassUiEvent.LoadInitialData(targetAccountId))
    }

    LaunchedEffect(viewModel.uiEffectFlow) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AssignClassUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is AssignClassUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    AzuraScreen(
        title = "Tugaskan Kelas",
        onBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    // Header: Target Account Info
                    item {
                        AzuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                                Text(
                                    text = "Petugas / Supervisor",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = uiState.targetAccount?.name ?: "Loading...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = uiState.targetAccount?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                )
                            }
                        }
                    }

                    // Selection Section
                    item {
                        Text(
                            text = "Pilih Kelas yang Diampu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            text = "Supervisor hanya dapat melakukan absensi pada kelas yang dipilih.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }

                    item {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AzuraSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.availableClasses.forEach { classItem ->
                                val isSelected = uiState.selectedClassIds.contains(classItem.id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onEvent(AssignClassUiEvent.ToggleClassSelection(classItem.id)) },
                                    label = { Text(classItem.name) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else {
                                        null
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }

                    // Selected List Summary
                    if (uiState.selectedClassIds.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = AzuraSpacing.sm))
                            Text(
                                text = "Daftar Terpilih (${uiState.selectedClassIds.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }

                        items(uiState.availableClasses.filter { it.id in uiState.selectedClassIds }) { classItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(text = classItem.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (uiState.error != null) {
                        item {
                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            ) {
                                Text(
                                    text = uiState.error!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(AzuraSpacing.md),
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(AzuraSpacing.md),
            ) {
                AzuraButton(
                    text = if (uiState.isSaving) "Menyimpan..." else "Simpan Perubahan",
                    onClick = { viewModel.onEvent(AssignClassUiEvent.SaveAssignments) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && !uiState.isLoading,
                    isLoading = uiState.isSaving,
                )
            }
        }
    }
}
