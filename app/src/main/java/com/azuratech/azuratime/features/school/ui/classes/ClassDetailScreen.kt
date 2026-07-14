package com.azuratech.azuratime.features.school.ui.classes

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.core.domain.model.TeacherAssignment
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.components.AddSessionDialog
import com.azuratech.azuratime.core.data.local.SessionWithDetails
import com.azuratech.azuratime.features.session.domain.model.SessionType

@Composable
fun ClassDetailScreen(
    classId: String,
    className: String,
    classViewModel: ClassViewModel,
    sessions: List<SessionWithDetails>,
    subjects: List<SubjectEntity>,
    classes: List<com.azuratech.azuraengine.model.ClassModel>,
    assignments: List<TeacherAssignment>,
    selectedTier: SessionType,
    onDeleteSession: (SessionWithDetails) -> Unit,
    onSelectTier: (SessionType) -> Unit,
    onAddSession: (String?, String?, SessionType, Int, String, String) -> Unit,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddStudent: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Students", "Schedules")

    val classState by classViewModel.uiStateFlow.collectAsStateWithLifecycle()

    var showAddSessionDialog by remember { mutableStateOf(false) }

    // Observe students for this specific class
    LaunchedEffect(classId) {
        classViewModel.onEvent(ClassUiEvent.SelectClass(classId))
    }

    AzuraScreen(
        title = className,
        onBack = onNavigateBack,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { classViewModel.onEvent(ClassUiEvent.ShowAddStudentDialog) }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Student")
                }
            } else {
                FloatingActionButton(onClick = { showAddSessionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Session")
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> StudentListSection(
                        students = classState.studentsInClass,
                        isLoading = classState.isLoading,
                    )
                    1 -> ClassScheduleSection(
                        classId = classId,
                        sessions = sessions.filter { it.classId == classId },
                        onDeleteSession = onDeleteSession,
                    )
                }
            }
        }
    }

    if (classState.isAddStudentDialogVisible) {
        AddStudentToClassDialog(
            allStudents = classState.allStudents,
            studentsInClass = classState.studentsInClass,
            onDismissRequest = { classViewModel.onEvent(ClassUiEvent.DismissAddStudentDialog) },
            onStudentSelected = { studentId ->
                classViewModel.onEvent(ClassUiEvent.AddStudentToClass(classId, studentId))
            },
        )
    }

    if (showAddSessionDialog) {
        AddSessionDialog(
            subjects = subjects,
            classes = classes,
            assignments = assignments,
            selectedTier = selectedTier,
            onTierSelected = onSelectTier,
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { clsId, subjId, tier, day, start, end ->
                onAddSession(clsId ?: classId, subjId, tier, day, start, end)
                showAddSessionDialog = false
            },
        )
    }
}

@Composable
fun ClassScheduleSection(
    @Suppress("UNUSED_PARAMETER") classId: String,
    sessions: List<com.azuratech.azuratime.core.data.local.SessionWithDetails>,
    onDeleteSession: (com.azuratech.azuratime.core.data.local.SessionWithDetails) -> Unit,
) {
    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No schedules found for this class.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            items(sessions) { sessionWithDetails ->
                AzuraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(AzuraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sessionWithDetails.subjectName ?: "Homeroom / General",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${getDayName(sessionWithDetails.dayOfWeek)} | ${sessionWithDetails.startTime} - ${sessionWithDetails.endTime}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Type: ${sessionWithDetails.sessionType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = { onDeleteSession(sessionWithDetails) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun getDayName(day: Int): String {
    return when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }
}
