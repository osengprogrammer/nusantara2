package com.azuratech.azuratime.ui.classes

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.ui.util.UiState

@Composable
fun ClassListScreen(
    classViewModel: ClassViewModel, // 🔥 Changed from OptionsViewModel
    onNavigateToDetail: (classId: String, className: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current 
    
    // 🔥 Consume UiState instead of raw list
    val uiState by classViewModel.uiState.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var classToDelete by remember { mutableStateOf<ClassEntity?>(null) }

    AzuraScreen(
        title = "Manajemen Kelas",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = AzuraShapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        }
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada kelas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is UiState.Success -> {
                val classes = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(top = AzuraSpacing.md, bottom = 100.dp)
                ) {
                    items(classes, key = { it.id }) { classItem ->
                        ClassItemCard(
                            classItem = classItem,
                            onClick = { onNavigateToDetail(classItem.id, classItem.name) },
                            onEdit = { classToEdit = classItem },
                            onDelete = { classToDelete = classItem }
                        )
                    }
                }
            }
        }

        // --- ➕ DIALOG ADD ---
        if (showAddDialog) {
            ClassInputDialog(
                title = "Tambah Kelas Baru",
                initialValue = "",
                onDismiss = { showAddDialog = false },
                onConfirm = { newName ->
                    classViewModel.addClass(newName) // 🔥 Simplified call
                    showAddDialog = false
                }
            )
        }

        // --- ✏️ DIALOG EDIT ---
        classToEdit?.let { item ->
            ClassInputDialog(
                title = "Edit Nama Kelas",
                initialValue = item.name,
                onDismiss = { classToEdit = null },
                onConfirm = { newName ->
                    classViewModel.updateClass(item.id, newName) // 🔥 Simplified call
                    classToEdit = null
                }
            )
        }

        // --- 🗑️ DIALOG DELETE ---
        classToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { classToDelete = null },
                icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Hapus Kelas?") },
                text = { Text("Menghapus '${item.name}' akan memutus hubungan dengan siswa di kelas ini.") },
                confirmButton = {
                    Button(
                        onClick = {
                            classViewModel.deleteClass(
                                classEntity = item,
                                onFailure = { msg ->
                                    context.showToast(msg) 
                                    classToDelete = null
                                },
                                onSuccess = {
                                    classToDelete = null
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Hapus") }
                },
                dismissButton = {
                    TextButton(onClick = { classToDelete = null }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun ClassItemCard(
    classItem: ClassEntity, // 🔥 Changed from OptionEntity
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(AzuraSpacing.md))
            
            Text(
                text = classItem.name, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Hapus", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ClassInputDialog(
    title: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nama Kelas") },
                placeholder = { Text("Contoh: 12-IPA-1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                shape = AzuraShapes.medium
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}