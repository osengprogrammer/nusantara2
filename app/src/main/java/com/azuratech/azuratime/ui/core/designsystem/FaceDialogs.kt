package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.ui.theme.AzuraShapes

@Composable
fun QuickEditFaceDialog(
    face: FaceEntity,
    onDismiss: () -> Unit,
    onSave: (FaceEntity) -> Unit
) {
    var editName by remember { mutableStateOf(face.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Singkat") },
        text = {
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(face.copy(name = editName)) }, shape = AzuraShapes.medium) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun MultiClassAssignmentDialog(
    studentName: String,
    allClasses: List<ClassEntity>,
    assignedClassIds: List<String>,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Kelas untuk $studentName", style = MaterialTheme.typography.titleMedium) },
        text = {
            if (allClasses.isEmpty()) {
                Text("Belum ada data kelas. Silakan tambahkan kelas terlebih dahulu.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allClasses) { clazz ->
                        val isAssigned = assignedClassIds.contains(clazz.id)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(clazz.id, !isAssigned) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Checkbox(
                                checked = isAssigned,
                                onCheckedChange = { checked -> onToggle(clazz.id, checked) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = clazz.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = AzuraShapes.medium) {
                Text("Selesai")
            }
        },
        shape = AzuraShapes.large
    )
}
