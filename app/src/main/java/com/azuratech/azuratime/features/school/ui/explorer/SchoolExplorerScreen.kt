package com.azuratech.azuratime.features.school.ui.explorer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.school.ui.classes.ClassViewModel
import com.azuratech.azuratime.features.school.ui.classes.ClassUiEvent
import com.azuratech.azuratime.features.school.ui.classes.ClassListSection
import com.azuratech.azuratime.features.school.ui.classes.AddClassDialog
import com.azuratech.azuratime.features.session.ui.SessionManagementViewModel
import com.azuratech.azuratime.features.session.ui.SessionManagementUiEvent
import com.azuratech.azuratime.features.session.ui.AddSubjectDialog
import com.azuratech.azuratime.features.account.ui.management.BulkAssignMatrixViewModel
import com.azuratech.azuratime.features.student.ui.StudentViewModel
import com.azuratech.azuratime.features.student.ui.StudentUiEvent
import com.azuratech.azuratime.features.student.ui.StudentUiState
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard

@Composable
fun SchoolExplorerScreen(
    @Suppress("UNUSED_PARAMETER") schoolId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: () -> Unit = {},
    navController: NavController? = null,
    classViewModel: ClassViewModel = hiltViewModel(),
    sessionViewModel: SessionManagementViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") matrixViewModel: BulkAssignMatrixViewModel = hiltViewModel(),
    studentViewModel: StudentViewModel = hiltViewModel(),
    initialTab: Int = 0,
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Classes", "Subjects", "Matrix", "Students")

    var showAddSubjectDialog by remember { mutableStateOf(false) }

    val classState by classViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val studentState by studentViewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "School Explorer",
        onBack = onNavigateBack,
        floatingActionButton = {
            ExplorerFab(
                selectedTab = selectedTab,
                onAddClass = { classViewModel.onEvent(ClassUiEvent.ShowAddDialog) },
                onAddSubject = { showAddSubjectDialog = true },
                onAddStudent = onNavigateToAddStudent,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = AzuraSpacing.md,
                divider = {},
            ) {
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
                    0 -> ClassListSection(
                        uiState = classState,
                        onClassClick = { classModel ->
                            navController?.navigate(Screen.ClassDetail.createRoute(classModel.id, classModel.name))
                        },
                        onEditClass = { classViewModel.onEvent(ClassUiEvent.RequestEditClass(it)) },
                        onDeleteClass = { classViewModel.onEvent(ClassUiEvent.RequestDeleteClass(it)) },
                    )
                    1 -> SubjectsTab(
                        subjects = sessionState.subjects,
                        onDeleteSubject = { sessionViewModel.onEvent(SessionManagementUiEvent.DeleteSubject(it)) },
                    )
                    2 -> MatrixTab()
                    3 -> StudentsTab(
                        state = studentState,
                        onEvent = studentViewModel::onEvent,
                    )
                }
            }
        }
    }

    // --- 🛠️ DIALOGS ---
    // Add Class
    if (classState.isAddDialogVisible) {
        AddClassDialog(
            availableClasses = classState.availableClasses,
            onDismissRequest = { classViewModel.onEvent(ClassUiEvent.DismissAddDialog) },
            onConfirmClick = { name ->
                classViewModel.onEvent(ClassUiEvent.CreateClass(name))
            },
        )
    }

    // Edit Class
    if (classState.classToEdit != null) {
        AddClassDialog(
            editingClass = classState.classToEdit,
            onDismissRequest = { classViewModel.onEvent(ClassUiEvent.CancelEditClass) },
            onConfirmClick = { newName ->
                classViewModel.onEvent(ClassUiEvent.UpdateClass(classState.classToEdit!!.id, newName))
            },
        )
    }

    // Delete Class Confirmation
    if (classState.classToDelete != null) {
        AlertDialog(
            onDismissRequest = { classViewModel.onEvent(ClassUiEvent.CancelDeleteClass) },
            title = { Text("Delete Class") },
            text = { Text("Are you sure you want to delete class '${classState.classToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { classViewModel.onEvent(ClassUiEvent.ConfirmDeleteClass) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { classViewModel.onEvent(ClassUiEvent.CancelDeleteClass) }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Add Subject
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            availableSubjects = sessionState.availableSubjects,
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, desc ->
                sessionViewModel.onEvent(SessionManagementUiEvent.AddSubject(name, desc))
                showAddSubjectDialog = false
            },
        )
    }
}

@Composable
private fun ExplorerFab(
    selectedTab: Int,
    onAddClass: () -> Unit,
    onAddSubject: () -> Unit,
    onAddStudent: () -> Unit,
) {
    when (selectedTab) {
        0 -> FloatingActionButton(onClick = onAddClass) { Icon(Icons.Default.Add, "Add Class") }
        1 -> FloatingActionButton(onClick = onAddSubject) { Icon(Icons.Default.Book, "Add Subject") }
        3 -> FloatingActionButton(onClick = onAddStudent) { Icon(Icons.Default.PersonAdd, "Add Student") }
    }
}

@Composable
fun SubjectsTab(
    subjects: List<com.azuratech.azuratime.features.session.data.local.SubjectEntity>,
    onDeleteSubject: (com.azuratech.azuratime.features.session.data.local.SubjectEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AzuraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
    ) {
        items(subjects) { subject ->
            ListItem(
                headlineContent = { Text(subject.name) },
                supportingContent = { subject.description?.let { Text(it) } },
                trailingContent = {
                    IconButton(onClick = { onDeleteSubject(subject) }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
    }
}

@Composable
fun MatrixTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.GridView, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(Modifier.height(AzuraSpacing.md))
            Text("Matrix Assignment", style = MaterialTheme.typography.titleMedium)
            Text("Manage teacher-class-subject relations.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StudentsTab(
    state: StudentUiState,
    onEvent: (StudentUiEvent) -> Unit,
) {
    if (state.isLoading && state.students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            items(state.students) { item ->
                StudentListItem(
                    item = item,
                    onEdit = { onEvent(StudentUiEvent.OpenEditDialog(item.profile)) },
                    onDelete = { onEvent(StudentUiEvent.DeleteStudent(item.profile.studentId)) },
                )
            }
        }
    }
}

@Composable
fun StudentListItem(
    item: StudentDisplayItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.profile.name, style = MaterialTheme.typography.titleMedium)
                Text("Class: ${item.assignedClassNames}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}
