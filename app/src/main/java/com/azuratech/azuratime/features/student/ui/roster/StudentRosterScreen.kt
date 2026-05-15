package com.azuratech.azuratime.features.student.ui.roster

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.student.ui.components.StudentRosterItem

@Composable
fun StudentRosterScreen(
    onEditStudentClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: StudentRosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Roster Siswa",
        onBack = onNavigateBack
    ) {
        when (val state = uiState) {
            is StudentRosterUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is StudentRosterUiState.Success -> {
                val data = state.data
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    AzuraTextField(
                        value = data.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = "Cari Siswa / ID",
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AzuraSpacing.md)
                    )

                    // Class Filter
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = AzuraSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
                    ) {
                        item {
                            FilterChip(
                                selected = data.selectedClassName == null,
                                onClick = { viewModel.onClassSelected(null) },
                                label = { Text("Semua") }
                            )
                        }
                        items(data.allClasses) { classModel ->
                            FilterChip(
                                selected = data.selectedClassName == classModel.name,
                                onClick = { viewModel.onClassSelected(classModel.id) },
                                label = { Text(classModel.name) }
                            )
                        }
                    }

                    // Student List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AzuraSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
                    ) {
                        items(data.students) { item ->
                            StudentRosterItem(
                                item = item,
                                onEditClick = { onEditStudentClick(item.profile.studentId) },
                                onDeleteClick = { viewModel.deleteStudent(item.profile.studentId) }
                            )
                        }
                        
                        if (data.students.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.xl), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada siswa ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            is StudentRosterUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
