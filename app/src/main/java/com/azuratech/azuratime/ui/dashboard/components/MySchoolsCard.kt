package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.school.AddSchoolDialog
import com.azuratech.azuratime.ui.school.SchoolUiState
import com.azuratech.azuratime.ui.school.SchoolViewModel
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun MySchoolsCard(
    viewModel: SchoolViewModel,
    accountId: String,
    isApproved: Boolean,
    onSchoolClick: () -> Unit,
    onAddSchool: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val schools = (uiState as? SchoolUiState.Success)?.schools ?: emptyList()
    val canAddMore = schools.isEmpty() || isApproved

    AzuraCard {
        Column(modifier = Modifier.padding(AzuraSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sekolah Saya",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canAddMore) {
                    IconButton(onClick = onAddSchool) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Sekolah", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = "Verifikasi diperlukan untuk menambah lebih dari 1 sekolah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }

            when (val state = uiState) {
                is SchoolUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is SchoolUiState.Success -> {
                    if (state.schools.isEmpty()) {
                        EmptySchoolsState(onAddClick = onAddSchool)
                    } else {
                        SchoolsHorizontalList(
                            schools = state.schools,
                            onSchoolClick = onSchoolClick
                        )
                    }
                }
                is SchoolUiState.Error -> {
                    Text(
                        text = "Gagal memuat data sekolah",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(AzuraSpacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
fun SchoolsHorizontalList(
    schools: List<School>,
    onSchoolClick: () -> Unit
) {
    Column {
        Text(
            text = "${schools.size} Sekolah Terdaftar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = AzuraSpacing.sm, vertical = 4.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(AzuraSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            items(schools, key = { it.id }) { school ->
                SchoolChip(school = school, onClick = onSchoolClick)
            }
        }
    }
}

@Composable
fun SchoolChip(school: School, onClick: () -> Unit) {
    val isActive = school.status == "ACTIVE"
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    Card(
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = Modifier
            .widthIn(min = 120.dp)
            .clickable(enabled = isActive) { 
                println("🖱️ DEBUG: Chip clicked for school ${school.name}")
                onClick() 
            }
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = school.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
                if (!isActive) {
                    Text(
                        text = school.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySchoolsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Belum ada sekolah terdaftar.", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onAddClick) {
            Text("Tambah Sekarang")
        }
    }
}
