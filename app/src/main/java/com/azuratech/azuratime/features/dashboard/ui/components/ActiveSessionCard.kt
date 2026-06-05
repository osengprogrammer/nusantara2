package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionCard(
    allClasses: List<ClassModel>,
    activeClassId: String?,
    onSelectClass: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val activeClassName = allClasses.find { it.id == activeClassId }?.name ?: "Select Class"

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(text = "Active Session", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = activeClassName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    allClasses.forEach { classItem ->
                        DropdownMenuItem(
                            text = { Text(classItem.name) },
                            onClick = {
                                onSelectClass(classItem.id)
                                expanded = false
                            },
                        )
                    }
                    if (activeClassId != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Clear Session / General Scan", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onSelectClass(null)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}
