package com.azuratech.azuratime.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun AddClassDialog(
    editingClass: ClassModel? = null,
    availableClasses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(editingClass?.name ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    val isNameValid = name.isNotBlank()
    
    val filteredClasses = remember(searchQuery, availableClasses) {
        if (searchQuery.isBlank()) availableClasses
        else availableClasses.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingClass == null) "Tambah Kelas" else "Ubah Nama Kelas") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                AzuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Kelas",
                    placeholder = "Contoh: 10-IPA-1",
                    errorText = if (name.isNotEmpty() && !isNameValid) "Nama tidak boleh kosong" else null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (editingClass == null) {
                    Spacer(modifier = Modifier.height(AzuraSpacing.xs))
                    Text(
                        text = "Atau Pilih dari Katalog:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // Search Bar for Katalog
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari kelas...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = AzuraShapes.medium,
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true
                    )

                    // Selection List
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        shape = AzuraShapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = AssistChipDefaults.assistChipBorder(enabled = true)
                    ) {
                        if (filteredClasses.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Belum ada kelas tersedia", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            LazyColumn {
                                items(filteredClasses) { className ->
                                    val isSelected = className == name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                name = className
                                                println("📚 DEBUG: Selected class from catalog: $className")
                                            }
                                            .padding(AzuraSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = className,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = AzuraSpacing.sm),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    println("✅ DEBUG: Confirming class: $name")
                    onConfirm(name) 
                },
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
