package com.azuratech.azuratime.ui.classes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

// 🔥 KMP Models
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.util.UiState
import com.azuratech.azuratime.ui.core.UiEvent

// 🔥 Azura Design System & Utils
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast

/**
 * 🏰 CLASS MANAGEMENT SCREEN
 * Refactored to use ClassModel and match School pattern.
 */
@Composable
fun ClassManagementScreen(
    viewModel: ClassViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onClassClick: (id: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI Event Collection
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }
    
    // State Observation
    val allClassState by viewModel.allAccountClasses.collectAsStateWithLifecycle()
    val schools by viewModel.schools.collectAsStateWithLifecycle()
    val availableClasses by viewModel.availableClasses.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    
    // UI Local State
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<ClassModel?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    // CSV Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                isImporting = true
                viewModel.importClassesFromCsv(selectedUri) {
                    isImporting = false
                    context.showToast("Impor Kelas Berhasil!")
                }
            }
        }
    }

    AzuraScreen(
        title = "Manajemen Kelas Terpusat",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingClass = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = AzuraShapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        },
        actions = {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 8.dp), 
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                IconButton(onClick = { csvLauncher.launch("text/*") }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.md),
                placeholder = { Text("Cari kelas atau sekolah...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = AzuraShapes.medium,
                singleLine = true
            )

            when (val state = allClassState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Belum ada data kelas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val filteredClasses: List<ClassModel> = remember(searchQuery, state.data, schools) {
                        state.data.filter { cls ->
                            val schoolName = schools.find { it.id == cls.schoolId }?.name ?: ""
                            cls.name.contains(searchQuery, ignoreCase = true) || 
                            schoolName.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        contentPadding = PaddingValues(start = AzuraSpacing.md, end = AzuraSpacing.md, bottom = 80.dp)
                    ) {
                        items(filteredClasses, key = { it.id }) { classItem ->
                            val schoolName = schools.find { it.id == classItem.schoolId }?.name
                            ClassItemRow(
                                classItem = classItem,
                                schoolName = schoolName,
                                onClick = { onClassClick(classItem.id, classItem.name) },
                                onEdit = {
                                    editingClass = classItem
                                    showDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteClass(
                                        classId = classItem.id,
                                        onFailure = { msg -> context.showToast(msg) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddClassDialog(
            editingClass = editingClass,
            availableClasses = availableClasses,
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                if (editingClass == null) {
                    user?.let { println("📡 DEBUG: Calling createClass with accountId=${it.userId}") }
                    viewModel.createClass(name, schoolId = null)
                } else {
                    viewModel.updateClass(editingClass!!.id, name)
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassItemRow(
    classItem: ClassModel,
    schoolName: String?,
    onClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classItem.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                if (schoolName != null) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(schoolName, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
