package com.azuratech.azuratime.features.session.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast

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

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is SessionManagementUiEffect.ShowToast -> context.showToast(effect.message)
            }
        }
    }

    AzuraScreen(
        title = stringResource(R.string.session_management),
        onBack = onNavigateBack,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
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
                    headlineContent = { Text(session.subjectName) },
                    supportingContent = {
                        Text("Day: ${getDayName(session.session.dayOfWeek)} | ${session.session.startTime} - ${session.session.endTime}")
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.onEvent(SessionManagementUiEvent.DeleteSession(session)) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
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
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { classId, subjectId, day, start, end ->
                viewModel.onEvent(SessionManagementUiEvent.AddSession(classId, subjectId, day, start, end))
                showAddSessionDialog = false
            },
        )
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subject)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.subject_description)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description.ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    subjects: List<com.azuratech.azuratime.features.session.data.local.SubjectEntity>,
    classes: List<com.azuratech.azuraengine.model.ClassModel>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String) -> Unit,
) {
    var selectedSubjectId by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_session)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                item {
                    Text(stringResource(R.string.select_subject), style = MaterialTheme.typography.labelMedium)
                    subjects.forEach { subj ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedSubjectId == subj.subjectId, onClick = { selectedSubjectId = subj.subjectId })
                            Text(subj.name)
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.select_class), style = MaterialTheme.typography.labelMedium)
                    classes.forEach { cls ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedClassId == cls.id, onClick = { selectedClassId = cls.id })
                            Text(cls.name)
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.select_day), style = MaterialTheme.typography.labelMedium)
                    (1..7).forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedDay == day, onClick = { selectedDay = day })
                            Text(getDayName(day))
                        }
                    }
                }
                item {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text(stringResource(R.string.start_time)) })
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text(stringResource(R.string.end_time)) })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedClassId, selectedSubjectId, selectedDay, startTime, endTime) },
                enabled = selectedSubjectId.isNotBlank() && selectedClassId.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun getDayName(day: Int): String = when (day) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "Unknown"
}
