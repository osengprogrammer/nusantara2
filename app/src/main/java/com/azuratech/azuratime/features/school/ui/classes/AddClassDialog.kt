package com.azuratech.azuratime.features.school.ui.classes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

// --- Parsed class name data class ---
data class ParsedClassName(
    val level: Int = 0,
    val category: String = "",
    val major: String = "",
    val section: String = "",
    val isValid: Boolean = false,
)

// --- Parser: quick-mode text -> structured fields ---
fun parseClassName(name: String): ParsedClassName {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return ParsedClassName()

    // Split on dashes, spaces, or both
    val parts = trimmed.split(Regex("[-\\s]+"))

    return when {
        // "Kelas 10 IPA 1" format
        parts.size >= 4 && parts[0].equals("Kelas", ignoreCase = true) -> {
            val level = parts[1].toIntOrNull() ?: 0
            val major = parts[2]
            val section = parts[3]
            val category = levelToCategory(level)
            ParsedClassName(level, category, major, section, level > 0 && major.isNotBlank())
        }
        // "10-IPA-1" or "10 IPA 1" format
        parts.size >= 3 && parts[0].toIntOrNull() != null -> {
            val level = parts[0].toIntOrNull() ?: 0
            val major = parts[1]
            val section = parts[2]
            val category = levelToCategory(level)
            ParsedClassName(level, category, major, section, level > 0 && major.isNotBlank())
        }
        else -> ParsedClassName()
    }
}

private fun levelToCategory(level: Int): String = when (level) {
    in 1..6 -> "SD"
    in 7..9 -> "SMP"
    in 10..12 -> "SMA"
    else -> ""
}

// ========================================================================
// Hybrid AddClassDialog
// ========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassDialog(
    editingClass: ClassModel? = null,
    availableClasses: List<String> = emptyList(),
    availableCategories: List<String> = emptyList(),
    availableMajors: List<String> = emptyList(),
    isStructuredMode: Boolean = false,
    onToggleMode: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onConfirmClick: (name: String, level: Int, category: String, major: String, section: String) -> Unit,
) {
    var name by remember { mutableStateOf(editingClass?.name ?: "") }
    var searchQuery by remember { mutableStateOf("") }

    // Structured mode state
    var selectedLevel by remember { mutableStateOf(editingClass?.level ?: 0) }
    var selectedCategory by remember { mutableStateOf(editingClass?.category ?: "") }
    var selectedMajor by remember { mutableStateOf(editingClass?.major ?: "") }
    var selectedSection by remember { mutableStateOf(editingClass?.section ?: "1") }

    // Auto-generate name from structured inputs
    val generatedName = remember(selectedLevel, selectedCategory, selectedMajor, selectedSection) {
        if (selectedLevel > 0 && selectedCategory.isNotBlank() && selectedMajor.isNotBlank()) {
            "Kelas $selectedLevel $selectedMajor $selectedSection"
        } else {
            ""
        }
    }

    // Auto-parse from quick mode input
    val parsedClass = remember(name) { parseClassName(name) }

    val isNameValid = name.isNotBlank() || (isStructuredMode && generatedName.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (editingClass == null) "Add Class" else "Edit Class") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (editingClass != null) {
                    // Edit mode — plain name input
                    AzuraTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Class Name",
                        placeholder = "Example: Kelas 10 IPA 1",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // Mode toggle chips
                    Text(
                        text = "Input Mode:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    ) {
                        FilterChip(
                            selected = !isStructuredMode,
                            onClick = { if (isStructuredMode) onToggleMode() },
                            label = { Text("Quick") },
                            leadingIcon = if (!isStructuredMode) {
                                { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                        )
                        FilterChip(
                            selected = isStructuredMode,
                            onClick = { if (!isStructuredMode) onToggleMode() },
                            label = { Text("Structured") },
                            leadingIcon = if (isStructuredMode) {
                                { Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(AzuraSpacing.xs))

                    if (!isStructuredMode) {
                        // ---- QUICK MODE ----
                        QuickModeInput(
                            name = name,
                            onNameChange = { name = it },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            availableClasses = availableClasses,
                            parsedClass = parsedClass,
                        )
                    } else {
                        // ---- STRUCTURED MODE ----
                        StructuredModeInput(
                            selectedLevel = selectedLevel,
                            onLevelChange = { selectedLevel = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            selectedMajor = selectedMajor,
                            onMajorChange = { selectedMajor = it },
                            selectedSection = selectedSection,
                            onSectionChange = { selectedSection = it },
                            availableCategories = availableCategories,
                            availableMajors = availableMajors,
                            generatedName = generatedName,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editingClass != null) {
                        onConfirmClick(name, editingClass.level, editingClass.category, editingClass.major, editingClass.section)
                    } else if (isStructuredMode) {
                        onConfirmClick(generatedName, selectedLevel, selectedCategory, selectedMajor, selectedSection)
                    } else {
                        onConfirmClick(name, parsedClass.level, parsedClass.category, parsedClass.major, parsedClass.section)
                    }
                },
                enabled = isNameValid,
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

// ========================================================================
// Quick Mode sub-component
// ========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickModeInput(
    name: String,
    onNameChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    availableClasses: List<String>,
    parsedClass: ParsedClassName,
) {
    val filteredClasses = remember(searchQuery, availableClasses) {
        if (searchQuery.isBlank()) {
            availableClasses
        } else {
            availableClasses.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Text(
        text = "Quick Mode — Type class name:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )

    AzuraTextField(
        value = name,
        onValueChange = onNameChange,
        label = "Class Name",
        placeholder = "Example: 10-IPA-1 or Kelas 10 IPA 1",
        modifier = Modifier.fillMaxWidth(),
    )

    // Auto-parsed preview
    if (name.isNotBlank() && parsedClass.isValid) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(AzuraSpacing.sm)) {
                Text(
                    "Auto-parsed:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text("Level: ${parsedClass.level}", style = MaterialTheme.typography.bodySmall)
                Text("Category: ${parsedClass.category}", style = MaterialTheme.typography.bodySmall)
                Text("Major: ${parsedClass.major}", style = MaterialTheme.typography.bodySmall)
                Text("Section: ${parsedClass.section}", style = MaterialTheme.typography.bodySmall)
            }
        }
    } else if (name.isNotBlank()) {
        Text(
            "Format: [level]-[major]-[section] or 'Kelas [level] [major] [section]'",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(modifier = Modifier.height(AzuraSpacing.xs))

    // Search & select from templates
    Text(
        text = "Or select from templates:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search class...", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
        shape = AzuraShapes.medium,
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp),
        shape = AzuraShapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        if (filteredClasses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classes available", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn {
                items(filteredClasses) { className ->
                    val isSelected = className == name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNameChange(className) }
                            .padding(AzuraSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = className,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Done,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ========================================================================
// Structured Mode sub-component
// ========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StructuredModeInput(
    selectedLevel: Int,
    onLevelChange: (Int) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedMajor: String,
    onMajorChange: (String) -> Unit,
    selectedSection: String,
    onSectionChange: (String) -> Unit,
    availableCategories: List<String>,
    availableMajors: List<String>,
    generatedName: String,
) {
    val levels = (1..12).toList()
    val sections = listOf("1", "2", "3", "A", "B", "C")

    Text(
        text = "Structured Mode — Select from dropdowns:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )

    // Level dropdown
    var levelExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = levelExpanded,
        onExpandedChange = { levelExpanded = it },
    ) {
        OutlinedTextField(
            value = if (selectedLevel > 0) "Kelas $selectedLevel" else "Select level",
            onValueChange = {},
            readOnly = true,
            label = { Text("Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = levelExpanded,
            onDismissRequest = { levelExpanded = false },
        ) {
            levels.forEach { level ->
                DropdownMenuItem(
                    text = { Text("Kelas $level") },
                    onClick = {
                        onLevelChange(level)
                        levelExpanded = false
                    },
                )
            }
        }
    }

    // Category dropdown
    var categoryExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = categoryExpanded,
        onExpandedChange = { categoryExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedCategory.ifEmpty { "Select category" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = categoryExpanded,
            onDismissRequest = { categoryExpanded = false },
        ) {
            availableCategories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategoryChange(category)
                        categoryExpanded = false
                    },
                )
            }
        }
    }

    // Major dropdown
    var majorExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = majorExpanded,
        onExpandedChange = { majorExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedMajor.ifEmpty { "Select major" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Major") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = majorExpanded,
            onDismissRequest = { majorExpanded = false },
        ) {
            availableMajors.forEach { major ->
                DropdownMenuItem(
                    text = { Text(major) },
                    onClick = {
                        onMajorChange(major)
                        majorExpanded = false
                    },
                )
            }
        }
    }

    // Section dropdown
    var sectionExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = sectionExpanded,
        onExpandedChange = { sectionExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedSection.ifEmpty { "Select section" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Section") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = sectionExpanded,
            onDismissRequest = { sectionExpanded = false },
        ) {
            sections.forEach { section ->
                DropdownMenuItem(
                    text = { Text(section) },
                    onClick = {
                        onSectionChange(section)
                        sectionExpanded = false
                    },
                )
            }
        }
    }

    // Auto-generated name preview
    if (generatedName.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.padding(AzuraSpacing.sm)) {
                Text(
                    "Auto-generated name:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    generatedName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
