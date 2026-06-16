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
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignClassScreen(
    targetAccountId: String,
    accountRole: AccountRole = AccountRole.USER,
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
        title = "Class Matrix Setup",
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
                            Row(modifier = Modifier.padding(AzuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                                StudentAvatar(photoPath = uiState.targetAccount?.photoUrl, size = 56.dp)
                                Spacer(modifier = Modifier.width(AzuraSpacing.md))
                                Column {
                                    Text(
                                        text = uiState.targetAccount?.name ?: "Loading...",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RoleBadge(roleStr = accountRole.name)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = uiState.targetAccount?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- STEP 1: SELECT CLASSES ---
                    item {
                        SectionHeader(
                            title = "Step 1: Select Classes",
                            subtitle = "Pick the classrooms this supervisor is responsible for.",
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            // Search & Bulk Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onEvent(AssignClassUiEvent.UpdateSearchQuery(it)) },
                                    placeholder = { Text("Filter classes...", style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = AzuraShapes.medium,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    ),
                                )
                                IconButton(onClick = { viewModel.onEvent(AssignClassUiEvent.SelectAllFiltered) }) {
                                    Icon(Icons.Default.SelectAll, "Select All", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.onEvent(AssignClassUiEvent.ClearAllSelections) }) {
                                    Icon(Icons.Default.ClearAll, "Clear All", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                uiState.filteredClasses.forEach { classItem ->
                                    val isSelected = uiState.selectedAssignments.any { it.classId == classItem.id }
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
                                if (uiState.filteredClasses.isEmpty()) {
                                    Text(
                                        "No classes match your search.",
                                        modifier = Modifier.padding(AzuraSpacing.md),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                    }

                    // --- STEP 2: DEFINE SUBJECT MATRIX ---
                    val selectedClasses = uiState.availableClasses.filter { cls ->
                        uiState.selectedAssignments.any { it.classId == cls.id }
                    }

                    if (selectedClasses.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Step 2: Assign Subjects",
                                subtitle = "Assign specific subjects or set as Wali Kelas (Homeroom).",
                            )
                        }

                        items(selectedClasses) { classItem ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = classItem.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Homeroom Option
                                    val isHomeroom = uiState.selectedAssignments.any { it.classId == classItem.id && it.subjectId == null }
                                    Surface(
                                        color = if (isHomeroom) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                                        shape = AzuraShapes.small,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                            Checkbox(
                                                checked = isHomeroom,
                                                onCheckedChange = { viewModel.onEvent(AssignClassUiEvent.ToggleClassSelection(classItem.id, null)) },
                                            )
                                            Text("Wali Kelas (Full Access)", style = MaterialTheme.typography.bodyMedium, fontWeight = if (isHomeroom) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }

                                    if (!isHomeroom) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            uiState.availableSubjects.forEach { subject ->
                                                val isSubjectSelected = uiState.selectedAssignments.any { it.classId == classItem.id && it.subjectId == subject.subjectId }
                                                FilterChip(
                                                    selected = isSubjectSelected,
                                                    onClick = { viewModel.onEvent(AssignClassUiEvent.ToggleClassSelection(classItem.id, subject.subjectId)) },
                                                    label = { Text(subject.name, style = MaterialTheme.typography.labelSmall) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- STEP 3: FINAL SUMMARY ---
                    if (uiState.selectedAssignments.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Final Assignment Summary",
                                subtitle = "Review the generated matrix before saving.",
                            )
                        }

                        item {
                            AzuraCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(AzuraSpacing.sm)) {
                                    uiState.selectedAssignments.forEach { assignment ->
                                        val classObj = uiState.availableClasses.find { it.id == assignment.classId }
                                        val subjectObj = uiState.availableSubjects.find { it.subjectId == assignment.subjectId }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = AzuraShapes.small,
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(6.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = classObj?.name ?: "Unknown",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(80.dp),
                                            )
                                            Icon(Icons.AutoMirrored.Filled.ArrowRight, null, tint = Color.LightGray)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = subjectObj?.name ?: "Full Access (Wali Kelas)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (subjectObj == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (subjectObj == null) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.error != null) {
                        item {
                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            ) {
                                Row(modifier = Modifier.padding(AzuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = uiState.error!!,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Bottom Action
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Box(modifier = Modifier.padding(AzuraSpacing.md)) {
                    AzuraButton(
                        text = if (uiState.isSaving) "Applying Matrix..." else "Confirm & Save Matrix",
                        onClick = { viewModel.onEvent(AssignClassUiEvent.SaveAssignments) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        isLoading = uiState.isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md, bottom = AzuraSpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = AzuraSpacing.xs), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
