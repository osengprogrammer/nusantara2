package com.azuratech.azuratime.features.session.ui
import com.azuratech.azuratime.core.domain.model.toSubjectTemplate
import com.azuratech.azuratime.core.domain.model.TeacherAssignment

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.azuratech.azuratime.core.domain.model.SubjectTemplate
import com.azuratech.azuratime.core.ui.components.AddSessionDialog
import com.azuratech.azuratime.core.ui.components.AddSubjectDialog
import com.azuratech.azuratime.core.ui.components.TierBadge
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(
    viewModel: SessionManagementViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<com.azuratech.azuratime.core.data.local.SessionWithDetails?>(null) }

    // Collect one‑off UI effects (e.g., toast from actions)
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is SessionManagementUiEffect.ShowToast -> context.showToast(effect.message)
                is SessionManagementUiEffect.ShowError -> context.showToast(effect.message)
            }
        }
    }

    AzuraScreen(
        title = stringResource(R.string.session_management),
        onBack = onNavigateBack,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (uiState.assignments.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.onEvent(SessionManagementUiEvent.GenerateFromMatrix) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Generate")
                    }
                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                }
                SmallFloatingActionButton(
                    onClick = { showAddSubjectDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                ExtendedFloatingActionButton(
                    onClick = { showAddSessionDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.add_session)) },
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            item {
                Text("Subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(uiState.subjects) { subject ->
                ListItem(
                    headlineContent = { Text(subject.name) },
                    supportingContent = { subject.description?.let { Text(it) } },
                    trailingContent = {
                        IconButton(onClick = { viewModel.onEvent(SessionManagementUiEvent.DeleteSubject(subject)) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(AzuraSpacing.md))
                Text("Scheduled Sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(uiState.sessions) { session ->
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.subjectName ?: session.sessionType.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(AzuraSpacing.sm))
                            TierBadge(session.sessionType)
                        }
                    },
                    supportingContent = {
                        Column(modifier = Modifier.animateContentSize()) {
                            Text("Day: ${getDayName(session.dayOfWeek)} | ${session.startTime} - ${session.endTime}")
                            if (session.sessionType != SessionType.GLOBAL) {
                                Text(
                                    text = "Class: ${session.className ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editingSession = session }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.onEvent(SessionManagementUiEvent.DeleteSession(session)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                )
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            availableSubjects = uiState.availableSubjects,
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, desc ->
                viewModel.onEvent(SessionManagementUiEvent.AddSubject(name, desc))
                showAddSubjectDialog = false
            },
        )
    }

    if (showAddSessionDialog) {
        AddSessionDialog(
            subjects = uiState.subjects,
            classes = uiState.availableClasses,
            assignments = uiState.assignments, // 🔥 Matrix Penugasan
            selectedTier = uiState.selectedTier,
            onTierSelected = { viewModel.onEvent(SessionManagementUiEvent.SelectTier(it)) },
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { classId, subjectId, tier, day, start, end ->
                viewModel.onEvent(SessionManagementUiEvent.AddSession(classId, subjectId, tier, day, start, end))
                showAddSessionDialog = false
            },
        )
    }

    if (editingSession != null) {
        AddSessionDialog(
            subjects = uiState.subjects,
            classes = uiState.availableClasses,
            assignments = uiState.assignments,
            selectedTier = uiState.selectedTier,
            onTierSelected = { viewModel.onEvent(SessionManagementUiEvent.SelectTier(it)) },
            editingSubjectId = editingSession?.subjectId,
            editingClassId = editingSession?.classId,
            editingDayOfWeek = editingSession?.dayOfWeek ?: 1,
            editingStartTime = editingSession?.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0),
            editingEndTime = editingSession?.endTime?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 30),
            editingTier = editingSession?.sessionType,
            onDismiss = { editingSession = null },
            onConfirm = { classId, subjectId, tier, day, start, end ->
                viewModel.onEvent(
                    SessionManagementUiEvent.UpdateSession(
                        sessionId = editingSession!!.sessionId,
                        classId = classId,
                        subjectId = subjectId,
                        sessionType = tier,
                        dayOfWeek = day,
                        startTime = start,
                        endTime = end,
                    ),
                )
                editingSession = null
            },
        )
    }
}

