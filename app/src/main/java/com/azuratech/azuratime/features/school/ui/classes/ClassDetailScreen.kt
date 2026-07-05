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
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.features.session.ui.SessionManagementViewModel
import com.azuratech.azuratime.features.session.ui.SessionManagementUiEvent
import com.azuratech.azuratime.features.session.ui.AddSessionDialog

@Composable
fun ClassDetailScreen(
    classId: String,
    className: String,
    classViewModel: ClassViewModel,
    sessionViewModel: SessionManagementViewModel,
    onNavigateBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Students", "Schedules")

    val classState by classViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.uiStateFlow.collectAsStateWithLifecycle()

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
                        sessions = sessionState.sessions.filter { it.session.classId == classId },
                        onDeleteSession = { sessionViewModel.onEvent(SessionManagementUiEvent.DeleteSession(it)) },
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
            subjects = sessionState.subjects,
            classes = sessionState.availableClasses,
            assignments = sessionState.assignments,
            selectedTier = sessionState.selectedTier,
            onTierSelected = { sessionViewModel.onEvent(SessionManagementUiEvent.SelectTier(it)) },
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { clsId, subjId, tier, day, start, end ->
                sessionViewModel.onEvent(
                    SessionManagementUiEvent.AddSession(
                        classId = clsId ?: classId,
                        subjectId = subjId,
                        sessionType = tier,
                        dayOfWeek = day,
                        startTime = start,
                        endTime = end,
                    ),
                )
                showAddSessionDialog = false
            },
        )
    }
}

@Composable
fun ClassScheduleSection(
    sessions: List<com.azuratech.azuratime.features.session.data.local.SessionWithDetails>,
    onDeleteSession: (com.azuratech.azuratime.features.session.data.local.SessionWithDetails) -> Unit,
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
                                text = "${getDayName(sessionWithDetails.session.dayOfWeek)} | ${sessionWithDetails.session.startTime} - ${sessionWithDetails.session.endTime}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "Type: ${sessionWithDetails.session.sessionType}",
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
