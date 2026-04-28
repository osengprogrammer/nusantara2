package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionCard(
    allClasses: List<ClassModel>,
    activeClassId: String?,
    onSelectClass: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val activeClassName = allClasses.find { it.id == activeClassId }?.name ?: "Pilih Kelas"

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(text = "Sesi Aktif", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = activeClassName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    allClasses.forEach { classItem ->
                        DropdownMenuItem(
                            text = { Text(classItem.name) },
                            onClick = {
                                onSelectClass(classItem.id)
                                expanded = false
                            }
                        )
                    }
                    if (activeClassId != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus Sesi / General Scan", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onSelectClass(null)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}