package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import androidx.compose.material.icons.filled.Person

@Composable
fun ClassManagementScreen(
    onClassSelected: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ClassViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            if (event is UiEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val title = if (uiState.selectedClassId != null) {
        val selectedClass = uiState.classes.find { it.id == uiState.selectedClassId }
        "Siswa: ${selectedClass?.name ?: "Kelas"}"
    } else {
        "Manajemen Kelas"
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
                    Icon(Icons.Default.Refresh, contentDescription = "Sinkronkan Kelas")
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedClassId == null) {
                FloatingActionButton(onClick = { viewModel.onEvent(ClassUiEvent.ShowAddDialog) }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
                }
            } else {
                FloatingActionButton(onClick = { viewModel.onEvent(ClassUiEvent.ShowAddStudentDialog) }) {
                    Icon(Icons.Default.Person, contentDescription = "Tambah Siswa")
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
                    onClassClick = { classModel ->
                        viewModel.onEvent(ClassUiEvent.SelectClass(classModel.id))
                    },
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
) {
    if (uiState.isLoading && uiState.classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.classes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada kelas yang terdaftar.")
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
            Text("Belum ada siswa di kelas ini.")
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
        title = { Text("Tambah Siswa ke Kelas") },
        text = {
            if (availableToAdd.isEmpty()) {
                Text("Semua siswa sudah ada di kelas ini.")
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
                Text("Tutup")
            }
        },
    )
}

@Composable
fun ClassItem(classModel: ClassModel, studentCount: Int, onClick: () -> Unit) {
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
            Column {
                Text(text = classModel.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$studentCount Siswa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
