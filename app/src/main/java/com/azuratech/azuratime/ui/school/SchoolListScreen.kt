package com.azuratech.azuratime.ui.school

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun SchoolListScreen(
    viewModel: SchoolViewModel = hiltViewModel(),
    onSchoolClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableClasses by viewModel.availableClasses.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    AzuraScreen(
        title = "Manajemen Sekolah",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Sekolah")
            }
        }
    ) {
        when (val state = uiState) {
            is SchoolUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SchoolUiState.Success -> {
                SchoolList(
                    schools = state.schools,
                    onSchoolClick = onSchoolClick,
                    onDeleteSchool = { viewModel.deleteSchool(it.id) },
                    onAddSchool = { showAddDialog = true }
                )
            }
            is SchoolUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.error.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showAddDialog) {
        AddSchoolDialog(
            availableClasses = availableClasses,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, timezone, selectedClassIds ->
                viewModel.createSchool(name, timezone, selectedClassIds)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddSchoolDialog(
    availableClasses: List<com.azuratech.azuraengine.model.ClassModel> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Jakarta") }
    var selectedClassIds by remember { mutableStateOf(setOf<String>()) }
    var classSearchQuery by remember { mutableStateOf("") }

    val filteredClasses = remember(classSearchQuery, availableClasses) {
        availableClasses.filter { it.name.contains(classSearchQuery, ignoreCase = true) }
    }

    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Sekolah Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                AzuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Sekolah",
                    errorText = if (name.isNotEmpty() && !isNameValid) "Nama tidak boleh kosong" else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                AzuraTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = "Timezone",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                Text("Pilih Kelas Tersedia", style = MaterialTheme.typography.titleSmall)

                if (availableClasses.isEmpty()) {
                    Text(
                        "Belum ada kelas. Buat di Manajemen Kelas dulu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    OutlinedTextField(
                        value = classSearchQuery,
                        onValueChange = { classSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari kelas...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = AzuraShapes.medium,
                        singleLine = true
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                        shape = AzuraShapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        LazyColumn {
                            items(filteredClasses) { classItem ->
                                val isSelected = selectedClassIds.contains(classItem.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedClassIds = if (isSelected) {
                                                selectedClassIds - classItem.id
                                            } else {
                                                selectedClassIds + classItem.id
                                            }
                                        }
                                        .padding(AzuraSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // Handled by row click
                                    )
                                    Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                                    Text(classItem.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, timezone, selectedClassIds.toList()) },
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

@Composable
fun SchoolList(
    schools: List<School>,
    onSchoolClick: (String) -> Unit,
    onDeleteSchool: (School) -> Unit,
    onAddSchool: () -> Unit
) {
    if (schools.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Belum ada sekolah.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(AzuraSpacing.md))
            Button(onClick = onAddSchool) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                Text("Tambah Sekolah")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            items(schools, key = { it.id }) { school ->
                SchoolItem(
                    school = school,
                    onClick = { onSchoolClick(school.id) },
                    onDelete = { onDeleteSchool(school) }
                )
            }
        }
    }
}

@Composable
fun SchoolItem(
    school: School,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    AzuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(school.name, style = MaterialTheme.typography.titleMedium)
                Text(school.timezone, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
