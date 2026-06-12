package com.azuratech.azuratime.features.session.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.session.domain.model.SessionType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.subjectName ?: session.session.sessionType.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(AzuraSpacing.sm))
                            TierBadge(session.session.sessionType)
                        }
                    },
                    supportingContent = {
                        Column(modifier = Modifier.animateContentSize()) {
                            Text("Day: ${getDayName(session.session.dayOfWeek)} | ${session.session.startTime} - ${session.session.endTime}")
                            if (session.session.sessionType != SessionType.GLOBAL) {
                                Text(
                                    text = "Class: ${session.className ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
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
            selectedTier = uiState.selectedTier, // ✅ Tier Selection state
            onTierSelected = { viewModel.onEvent(SessionManagementUiEvent.SelectTier(it)) },
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { classId, subjectId, tier, day, start, end ->
                viewModel.onEvent(SessionManagementUiEvent.AddSession(classId, subjectId, tier, day, start, end))
                showAddSessionDialog = false
            },
        )
    }
}

@Composable
fun TierBadge(type: SessionType) {
    val (color, label) = when (type) {
        SessionType.ACADEMIC -> MaterialTheme.colorScheme.primary to "Academic"
        SessionType.CLASS_WIDE -> MaterialTheme.colorScheme.secondary to "Class"
        SessionType.GLOBAL -> MaterialTheme.colorScheme.tertiary to "Global"
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
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
    selectedTier: SessionType,
    onTierSelected: (SessionType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, SessionType, Int, String, String) -> Unit,
) {
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(9, 30)) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_session)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier.fillMaxHeight(0.8f).animateContentSize(),
            ) {
                // Tier Selector
                item {
                    Text("Session Tier", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs)) {
                        SessionType.entries.forEach { tier ->
                            FilterChip(
                                selected = selectedTier == tier,
                                onClick = { onTierSelected(tier) },
                                label = { Text(tier.name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                // Conditional Subject Picker
                if (selectedTier == SessionType.ACADEMIC) {
                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                        Text(stringResource(R.string.select_subject), style = MaterialTheme.typography.titleSmall)
                        subjects.forEach { subj ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedSubjectId = subj.subjectId },
                            ) {
                                RadioButton(selected = selectedSubjectId == subj.subjectId, onClick = { selectedSubjectId = subj.subjectId })
                                Text(subj.name)
                            }
                        }
                    }
                }

                // Conditional Class Picker
                if (selectedTier != SessionType.GLOBAL) {
                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                        Text(stringResource(R.string.select_class), style = MaterialTheme.typography.titleSmall)
                        classes.forEach { cls ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { selectedClassId = cls.id },
                            ) {
                                RadioButton(selected = selectedClassId == cls.id, onClick = { selectedClassId = cls.id })
                                Text(cls.name)
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                    Text(stringResource(R.string.select_day), style = MaterialTheme.typography.titleSmall)
                    (1..7).forEach { day ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedDay = day },
                        ) {
                            RadioButton(selected = selectedDay == day, onClick = { selectedDay = day })
                            Text(getDayName(day))
                        }
                    }
                }
                item {
                    HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                    Text("Time Range", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                    ) {
                        OutlinedCard(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.medium,
                        ) {
                            Column(Modifier.padding(AzuraSpacing.md)) {
                                Text(stringResource(R.string.start_time), style = MaterialTheme.typography.labelSmall)
                                Text(startTime.format(timeFormatter), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        OutlinedCard(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.medium,
                        ) {
                            Column(Modifier.padding(AzuraSpacing.md)) {
                                Text(stringResource(R.string.end_time), style = MaterialTheme.typography.labelSmall)
                                Text(endTime.format(timeFormatter), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isFormValid = when (selectedTier) {
                SessionType.ACADEMIC -> selectedSubjectId != null && selectedClassId != null
                SessionType.CLASS_WIDE -> selectedClassId != null
                SessionType.GLOBAL -> true
            }
            Button(
                onClick = {
                    onConfirm(
                        if (selectedTier != SessionType.GLOBAL) selectedClassId else null,
                        if (selectedTier == SessionType.ACADEMIC) selectedSubjectId else null,
                        selectedTier,
                        selectedDay,
                        startTime.format(timeFormatter),
                        endTime.format(timeFormatter),
                    )
                },
                enabled = isFormValid,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (showStartPicker) {
        AzuraTimePickerDialog(
            initialTime = startTime,
            onDismiss = { showStartPicker = false },
            onTimeSelected = {
                startTime = it
                showStartPicker = false
            },
        )
    }

    if (showEndPicker) {
        AzuraTimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndPicker = false },
            onTimeSelected = {
                endTime = it
                showEndPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        text = {
            TimePicker(state = timePickerState)
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
