package com.azuratech.azuratime.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSchoolDialog(
    availableClasses: List<ClassModel> = emptyList(),
    onDismissRequest: () -> Unit,
    onConfirmClick: (String, String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Jakarta") }
    val selectedClassIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add New School") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                AzuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "School Name",
                    modifier = Modifier.fillMaxWidth(),
                )
                AzuraTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = "Timezone",
                    modifier = Modifier.fillMaxWidth(),
                )

                if (availableClasses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                    Text(
                        text = "Select Classes (Optional):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Select existing classes to move to this school.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(availableClasses) { classModel ->
                            val isSelected = selectedClassIds.contains(classModel.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedClassIds.remove(classModel.id)
                                    } else {
                                        selectedClassIds.add(classModel.id)
                                    }
                                },
                                label = { Text(classModel.name) },
                                shape = AzuraShapes.small,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmClick(name, timezone, selectedClassIds.toList()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
