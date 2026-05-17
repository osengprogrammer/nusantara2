package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun MySchoolsCard(
    viewModel: SchoolViewModel,
    accountId: String,
    @Suppress("UNUSED_PARAMETER") isApproved: Boolean,
    @Suppress("UNUSED_PARAMETER") globalRole: String,
    onSchoolClick: (String) -> Unit,
    onAddSchoolClick: () -> Unit,
) {
    val schoolUiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val schools = schoolUiState.schools
    val isLoading = schoolUiState.isLoading

    AzuraCard(
        title = "Sekolah Saya",
        modifier = Modifier.fillMaxWidth(),
        actions = {
            IconButton(onClick = { viewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.LoadSchools(accountId)) }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Refresh Schools",
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            if (isLoading && schools.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.lg), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (schools.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                ) {
                    Text(
                        "Anda belum terdaftar di sekolah manapun.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(AzuraSpacing.xs))
                    Button(
                        onClick = onAddSchoolClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzuraShapes.medium,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(AzuraSpacing.sm))
                        Text("Buat Sekolah Baru")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: Navigate to Join/Search School Screen */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzuraShapes.medium,
                    ) {
                        Text("Gabung Sekolah")
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(vertical = AzuraSpacing.xs),
                ) {
                    items(schools) { school ->
                        SchoolItem(school = school, onClick = { onSchoolClick(school.id) })
                    }
                }

                if (globalRole == "SUPER_ADMIN" || globalRole == "ADMIN") {
                    Button(
                        onClick = onAddSchoolClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzuraShapes.medium,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(AzuraSpacing.sm))
                        Text("Tambah Sekolah")
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolItem(school: com.azuratech.azuraengine.model.School, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = AzuraShapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = AssistChipDefaults.assistChipBorder(enabled = true),
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(AzuraSpacing.xs))
            Text(
                text = school.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = school.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (school.status == "ACTIVE") Color(0xFF2E7D32) else Color.Gray,
            )
        }
    }
}
