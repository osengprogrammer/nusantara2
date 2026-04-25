package com.azuratech.azuratime.ui.school

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun SchoolListScreen(
    accountId: String,
    viewModel: SchoolViewModel = hiltViewModel(),
    onSchoolClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        viewModel.loadSchools(accountId)
    }

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
                    onDeleteSchool = { viewModel.deleteSchool(it.id, accountId) }
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
            onDismiss = { showAddDialog = false },
            onConfirm = { name, timezone ->
                viewModel.addSchool(accountId, name, timezone)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddSchoolDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Jakarta") }
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, timezone) },
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
    onDeleteSchool: (School) -> Unit
) {
    if (schools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada sekolah.")
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
