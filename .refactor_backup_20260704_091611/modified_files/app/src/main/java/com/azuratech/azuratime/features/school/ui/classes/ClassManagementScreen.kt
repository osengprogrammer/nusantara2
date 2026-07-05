package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.R
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import androidx.compose.material.icons.filled.Person

@Composable
fun ClassManagementScreen(
    onNavigateBack: () -> Unit,
    onClassClick: (ClassModel) -> Unit = {},
    viewModel: ClassViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is ClassUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ClassUiEffect.NavigateTo -> {} // Handle if needed
            }
        }
    }

    val title = if (uiState.selectedClassId != null) {
        val selectedClass = uiState.classes.find { it.id == uiState.selectedClassId }
        stringResource(R.string.label_user_plural).split(":").first() + ": ${selectedClass?.name ?: stringResource(R.string.label_session_singular)}"
    } else {
        stringResource(R.string.label_session_singular).substringBefore(" ") + " " + stringResource(R.string.label_management)
    }

    AzuraScreen(
        title = title,
        onBack = {
            if (uiState.selectedClassId != null) {
                viewModel.onEvent(ClassUiEvent.SelectClass(null))
            } else {
                onNavigateBack()
            }
        },
        snackbarHostState = snackbarHostState,
        actions = {
            if (uiState.selectedClassId == null) {
                IconButton(
                    onClick = { viewModel.onEvent(ClassUiEvent.SyncClasses) },
                    enabled = !uiState.isLoading,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_sync))
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedClassId == null) {
                FloatingActionButton(onClick = { viewModel.onEvent(ClassUiEvent.ShowAddDialog) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_session))
                }
            } else {
                FloatingActionButton(onClick = { viewModel.onEvent(ClassUiEvent.ShowAddStudentDialog) }) {
                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.action_add_user_singular))
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.selectedClassId != null) {
                StudentListSection(
                    students = uiState.studentsInClass,
                    isLoading = uiState.isLoading,
                )
            } else {
                ClassListSection(
                    uiState = uiState,
                    onClassClick = onClassClick,
                    onEditClass = { viewModel.onEvent(ClassUiEvent.RequestEditClass(it)) },
                    onDeleteClass = { viewModel.onEvent(ClassUiEvent.RequestDeleteClass(it)) },
                )
            }
        }
    }

    if (uiState.isAddDialogVisible) {
        AddClassDialog(
            availableClasses = uiState.availableClasses,
            onDismissRequest = { viewModel.onEvent(ClassUiEvent.DismissAddDialog) },
            onConfirmClick = { name ->
                viewModel.onEvent(ClassUiEvent.CreateClass(name))
            },
        )
    }

    if (uiState.isAddStudentDialogVisible && uiState.selectedClassId != null) {
        AddStudentToClassDialog(
            allStudents = uiState.allStudents,
            studentsInClass = uiState.studentsInClass,
            onDismissRequest = { viewModel.onEvent(ClassUiEvent.DismissAddStudentDialog) },
            onStudentSelected = { studentId ->
                viewModel.onEvent(ClassUiEvent.AddStudentToClass(uiState.selectedClassId!!, studentId))
                viewModel.onEvent(ClassUiEvent.DismissAddStudentDialog)
            },
        )
    }
}

@Composable
fun ClassListSection(
    uiState: ClassUiState,
    onClassClick: (ClassModel) -> Unit,
    onEditClass: (ClassModel) -> Unit = {},
    onDeleteClass: (ClassModel) -> Unit = {},
) {
    if (uiState.isLoading && uiState.classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_sessions_registered))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            items(uiState.classes) { classModel ->
                val studentCount = uiState.studentCountsByClassId[classModel.id] ?: 0
                ClassItem(
                    classModel = classModel,
                    studentCount = studentCount,
                    onClick = { onClassClick(classModel) },
                    onEdit = { onEditClass(classModel) },
                    onDelete = { onDeleteClass(classModel) },
                )
            }
        }
    }
}

@Composable
fun StudentListSection(
    students: List<StudentProfile>,
    isLoading: Boolean,
) {
    if (isLoading && students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_users_in_session))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            items(students) { student ->
                StudentItem(student = student)
            }
        }
    }
}

@Composable
fun StudentItem(student: StudentProfile) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column {
                Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AddStudentToClassDialog(
    allStudents: List<StudentProfile>,
    studentsInClass: List<StudentProfile>,
    onDismissRequest: () -> Unit,
    onStudentSelected: (String) -> Unit,
) {
    val availableToAdd = allStudents.filter { student ->
        studentsInClass.none { it.studentId == student.studentId }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.dialog_user_to_session_title)) },
        text = {
            if (availableToAdd.isEmpty()) {
                Text(stringResource(R.string.empty_users_already_in_session))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(availableToAdd) { student ->
                        Text(
                            text = student.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStudentSelected(student.studentId) }
                                .padding(AzuraSpacing.md),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
fun ClassItem(
    classModel: ClassModel,
    studentCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    AzuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Class, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = classModel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$studentCount ${stringResource(R.string.label_user_plural)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, stringResource(R.string.action_edit), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.action_delete), modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
