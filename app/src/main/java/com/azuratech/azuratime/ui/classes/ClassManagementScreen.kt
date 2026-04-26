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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagementScreen(
    viewModel: ClassViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onClassClick: (id: String, name: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State Observation
    val classes by viewModel.classes.collectAsStateWithLifecycle(emptyList())
    
    // UI Local State
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
        title = "Manajemen Kelas",
        onBack = onNavigateBack,
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
        Column(modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.md)) {
            
            Text(
                text = "Daftar Kelas Aktif",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            if (classes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada data kelas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(classes, key = { it.id }) { classItem ->
                        ClassItemRow(
                            classItem = classItem,
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

    if (showDialog) {
        AddEditClassDialog(
            editingClass = editingClass,
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                if (editingClass == null) {
                    viewModel.addClass(name)
                } else {
                    viewModel.updateClass(editingClass!!.id, name)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun AddEditClassDialog(
    editingClass: ClassModel?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(editingClass?.name ?: "") }
    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingClass == null) "Tambah Kelas" else "Ubah Nama Kelas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                AzuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Kelas",
                    placeholder = "Contoh: 10-IPA-1",
                    errorText = if (name.isNotEmpty() && !isNameValid) "Nama tidak boleh kosong" else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = isNameValid
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassItemRow(
    classItem: ClassModel,
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
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(AzuraSpacing.md))
            Text(
                text = classItem.name, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
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
