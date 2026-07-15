package com.azuratech.azuratime.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.domain.model.TeacherAssignment
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.session.domain.model.SessionType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    subjects: List<SubjectEntity>,
    classes: List<ClassModel>,
    assignments: List<TeacherAssignment>,
    selectedTier: SessionType,
    onTierSelected: (SessionType) -> Unit,
    editingSubjectId: String? = null,
    editingClassId: String? = null,
    editingDayOfWeek: Int = 1,
    editingStartTime: LocalTime = LocalTime.of(8, 0),
    editingEndTime: LocalTime = LocalTime.of(9, 30),
    editingTier: SessionType? = null,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, SessionType, Int, String, String) -> Unit,
) {
    val isEditing = editingSubjectId != null || editingClassId != null

    LaunchedEffect(editingTier) {
        editingTier?.let { onTierSelected(it) }
    }

    var selectedSubjectId by remember(editingSubjectId) { mutableStateOf(editingSubjectId) }
    var selectedClassId by remember(editingClassId) { mutableStateOf(editingClassId) }
    var selectedDay by remember(editingDayOfWeek) { mutableIntStateOf(editingDayOfWeek) }
    var startTime by remember(editingStartTime) { mutableStateOf(editingStartTime) }
    var endTime by remember(editingEndTime) { mutableStateOf(editingEndTime) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Session" else "Add Session") },
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

                // Matrix Picker for ACADEMIC sessions
                if (selectedTier == SessionType.ACADEMIC && assignments.isNotEmpty()) {
                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                        Text("My Assignments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
                    // 🎯 NEW: Conditional Subject Picker as ExposedDropdownMenu
                    if (selectedTier == SessionType.ACADEMIC) {
                        item {
                            HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                            var expandedSubject by remember { mutableStateOf(false) }
                            val selectedSubjectName = subjects.find { it.subjectId == selectedSubjectId }?.name ?: "Select a Subject"
                            
                            ExposedDropdownMenuBox(
                                expanded = expandedSubject,
                                onExpandedChange = { expandedSubject = !expandedSubject }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubjectName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subject") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubject) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    shape = AzuraShapes.medium
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedSubject,
                                    onDismissRequest = { expandedSubject = false }
                                ) {
                                    subjects.forEach { subj ->
                                        DropdownMenuItem(
                                            text = { Text(subj.name) },
                                            onClick = {
                                                selectedSubjectId = subj.subjectId
                                                expandedSubject = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Conditional Class Picker as ExposedDropdownMenu
                    if (selectedTier != SessionType.GLOBAL) {
                        item {
                            HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                            var expandedClass by remember { mutableStateOf(false) }
                            val selectedClassName = classes.find { it.id == selectedClassId }?.name ?: "Select a Class"
                            
                            ExposedDropdownMenuBox(
                                expanded = expandedClass,
                                onExpandedChange = { expandedClass = !expandedClass }
                            ) {
                                OutlinedTextField(
                                    value = selectedClassName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Class") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    shape = AzuraShapes.medium
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedClass,
                                    onDismissRequest = { expandedClass = false }
                                ) {
                                    classes.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls.name) },
                                            onClick = {
                                                selectedClassId = cls.id
                                                expandedClass = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.sm))
                    Text("Select Day", style = MaterialTheme.typography.titleSmall)
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
                                Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                Text(startTime.format(timeFormatter), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        OutlinedCard(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = AzuraShapes.medium,
                        ) {
                            Column(Modifier.padding(AzuraSpacing.md)) {
                                Text("End Time", style = MaterialTheme.typography.labelSmall)
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                Text("Cancel")
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
