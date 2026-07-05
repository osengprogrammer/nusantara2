package com.azuratech.azuratime.features.session.ui

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
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.azuratech.azuratime.features.template.domain.model.SubjectTemplate
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
    var editingSession by remember { mutableStateOf<com.azuratech.azuratime.features.session.data.local.SessionWithDetails?>(null) }

    // Collect one‑off UI effects (e.g., toast from actions)
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is SessionManagementUiEffect.ShowToast -> context.showToast(effect.message)
            }
        }
    }

    // State‑driven error handling: show toast/snackbar when uiState.error is set
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            context.showToast(msg)
            // Clear the error after showing it so it does not re‑appear on recomposition/rotation
            viewModel.onEvent(SessionManagementUiEvent.ClearError)
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
                        Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.action_auto_generate))
                    }
                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                }
                SmallFloatingActionButton(
                    onClick = { showAddSubjectDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_subject))
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
                Text(stringResource(R.string.label_task_plural), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(uiState.subjects) { subject ->
                ListItem(
                    headlineContent = { Text(subject.name) },
                    supportingContent = { subject.description?.let { Text(it) } },
                    trailingContent = {
                        IconButton(onClick = { viewModel.onEvent(SessionManagementUiEvent.DeleteSubject(subject)) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(AzuraSpacing.md))
                Text(stringResource(R.string.label_session_singular).substringBefore(" ") + "s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.label_level).substringBefore(" ") + ": ${getDayName(session.session.dayOfWeek)} | ${session.session.startTime} - ${session.session.endTime}")
                            if (session.session.sessionType != SessionType.GLOBAL) {
                                Text(
                                    text = "${stringResource(R.string.label_session_singular)}: ${session.className ?: stringResource(R.string.empty_sessions_available)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editingSession = session }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.onEvent(SessionManagementUiEvent.DeleteSession(session)) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
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
            editingSession = editingSession?.session,
            onDismiss = { editingSession = null },
            onConfirm = { classId, subjectId, tier, day, start, end ->
                viewModel.onEvent(
                    SessionManagementUiEvent.UpdateSession(
                        sessionId = editingSession!!.session.sessionId,
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

@Composable
fun AddSubjectDialog(
    availableSubjects: List<SubjectTemplate> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val isNameValid = name.isNotBlank()

    val filteredSubjects = remember(searchQuery, availableSubjects) {
        if (searchQuery.isBlank()) {
            availableSubjects
        } else {
            availableSubjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subject)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (name.isNotEmpty()) {
                    Surface(
                        shape = AzuraShapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = AzuraSpacing.xs),
                    ) {
                        Text(
                            text = "Selected Subject: $name",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AzuraSpacing.xs))
                Text(
                    text = stringResource(R.string.label_task_singular).substringBefore(" ") + " Selection:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                // Search Bar for Catalog
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search " + stringResource(R.string.label_task_singular).split(" ").last() + "...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = AzuraShapes.medium,
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                )

                // Selection List
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = AssistChipDefaults.assistChipBorder(enabled = true),
                ) {
                    val showCustomOption = searchQuery.isNotBlank() && availableSubjects.none { it.name.equals(searchQuery, ignoreCase = true) }

                    if (filteredSubjects.isEmpty() && !showCustomOption) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No " + stringResource(R.string.label_task_plural).substringBefore(" ") + " available", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn {
                            if (showCustomOption) {
                                item {
                                    val isSelected = searchQuery == name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                name = searchQuery
                                                println("📚 DEBUG: Using custom subject: $searchQuery")
                                            }
                                            .padding(AzuraSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = "Create custom: \"$searchQuery\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            )
                                            Text(
                                                text = "User-defined custom subject",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = AzuraSpacing.sm),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }

                            items(filteredSubjects) { template ->
                                val isSelected = template.name == name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = template.name
                                            println("📚 DEBUG: Selected subject from catalog: ${template.name}")
                                        }
                                        .padding(AzuraSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (template.category.isNotBlank()) {
                                            Text(
                                                text = template.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AzuraSpacing.sm),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, null) },
                enabled = isNameValid,
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
    assignments: List<com.azuratech.azuratime.features.account.domain.model.TeacherAssignment>,
    selectedTier: SessionType,
    onTierSelected: (SessionType) -> Unit,
    editingSession: com.azuratech.azuratime.features.session.data.local.ClassSessionEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, SessionType, Int, String, String) -> Unit,
) {
    LaunchedEffect(editingSession) {
        editingSession?.let {
            onTierSelected(it.sessionType)
        }
    }

    var selectedSubjectId by remember(editingSession) { mutableStateOf(editingSession?.subjectId) }
    var selectedClassId by remember(editingSession) { mutableStateOf(editingSession?.classId) }
    var selectedDay by remember(editingSession) { mutableIntStateOf(editingSession?.dayOfWeek ?: 1) }
    var startTime by remember(editingSession) {
        mutableStateOf(editingSession?.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0))
    }
    var endTime by remember(editingSession) {
        mutableStateOf(editingSession?.endTime?.let { LocalTime.parse(it) } ?: LocalTime.of(9, 30))
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSession != null) stringResource(R.string.dialog_delete_session_title) else stringResource(R.string.add_session)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier.fillMaxHeight(0.8f).animateContentSize(),
            ) {
                // Tier Selector
                item {
                    Text(stringResource(R.string.label_session_tier), style = MaterialTheme.typography.titleSmall)
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

                // Matrix Picker for ACADEMIC sessions
                if (selectedTier == SessionType.ACADEMIC && assignments.isNotEmpty()) {
                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                        Text(stringResource(R.string.label_my_assignments), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        assignments.forEach { assignment ->
                            val classObj = classes.find { it.id == assignment.classId }
                            val subjectObj = subjects.find { it.subjectId == assignment.subjectId }
                            val label = "${classObj?.name ?: "Unknown Class"} - ${subjectObj?.name ?: "Homeroom"}"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedClassId = assignment.classId
                                    selectedSubjectId = assignment.subjectId
                                },
                            ) {
                                RadioButton(
                                    selected = selectedClassId == assignment.classId && selectedSubjectId == assignment.subjectId,
                                    onClick = {
                                        selectedClassId = assignment.classId
                                        selectedSubjectId = assignment.subjectId
                                    },
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    // Fallback to manual selection if no assignments or different tier
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
                    Text(stringResource(R.string.label_time_range), style = MaterialTheme.typography.titleSmall)
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
