package com.azuratech.azuratime.core.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import com.azuratech.azuratime.core.domain.model.SubjectTemplate
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun AddSubjectDialog(
    availableSubjects: List<SubjectTemplate> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val isNameValid = name.isNotBlank()

    val filteredSubjects = remember(searchQuery, availableSubjects) {
        if (searchQuery.isBlank()) {
            availableSubjects
        } else {
            availableSubjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (name.isNotEmpty()) {
                    Surface(
                        shape = AzuraShapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = AzuraSpacing.xs),
                    ) {
                        Text(
                            text = "Selected Subject: $name",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AzuraSpacing.xs))
                Text(
                    text = "Select Subject from Templates:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search subject...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = AzuraShapes.medium,
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    shape = AzuraShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = AssistChipDefaults.assistChipBorder(enabled = true),
                ) {
                    val showCustomOption = searchQuery.isNotBlank() && availableSubjects.none { it.name.equals(searchQuery, ignoreCase = true) }

                    if (filteredSubjects.isEmpty() && !showCustomOption) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No subjects available", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn {
                            if (showCustomOption) {
                                item {
                                    val isSelected = searchQuery == name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                name = searchQuery
                                                println("📚 DEBUG: Using custom subject: $searchQuery")
                                            }
                                            .padding(AzuraSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = "Create custom: \"$searchQuery\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            )
                                            Text(
                                                text = "User-defined custom subject",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = AzuraSpacing.sm),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }

                            items(filteredSubjects) { template ->
                                val isSelected = template.name == name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = template.name
                                            println("📚 DEBUG: Selected subject from catalog: ${template.name}")
                                        }
                                        .padding(AzuraSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (template.category.isNotBlank()) {
                                            Text(
                                                text = template.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = AzuraSpacing.sm),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, null) },
                enabled = isNameValid,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
