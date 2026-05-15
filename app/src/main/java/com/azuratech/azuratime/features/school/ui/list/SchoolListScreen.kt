package com.azuratech.azuratime.features.school.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun SchoolListScreen(
    viewModel: SchoolViewModel = hiltViewModel(),
    onSchoolClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val schools by viewModel.schools.collectAsStateWithLifecycle()
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
        // School list rendering here
    }

    if (showAddDialog) {
        AddSchoolDialog(
            availableClasses = availableClasses,
            onDismissRequest = { showAddDialog = false },
            onConfirmClick = { name, timezone, selectedClassIds ->
                viewModel.createSchool(name, timezone, selectedClassIds)
                showAddDialog = false
            }
        )
    }
}
