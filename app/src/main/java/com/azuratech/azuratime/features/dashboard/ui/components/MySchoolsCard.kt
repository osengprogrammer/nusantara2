package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
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
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.school.ui.list.AddSchoolDialog
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel

@Composable
fun MySchoolsCard(
    viewModel: SchoolViewModel,
    @Suppress("UNUSED_PARAMETER") accountId: String,
    @Suppress("UNUSED_PARAMETER") isApproved: Boolean,
    @Suppress("UNUSED_PARAMETER") globalRole: String,
    onSchoolClick: (String) -> Unit,
    onAddSchoolClick: () -> Unit
) {
    val schools by viewModel.allSchools.collectAsStateWithLifecycle()

    AzuraCard(
        title = "Sekolah Saya",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            if (schools.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
                ) {
                    Text(
                        "Anda belum terdaftar di sekolah manapun.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(AzuraSpacing.xs))
                    Button(
                        onClick = onAddSchoolClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzuraShapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(AzuraSpacing.sm))
                        Text("Buat Sekolah Baru")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: Navigate to Join/Search School Screen */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AzuraShapes.medium
                    ) {
                        Text("Gabung Sekolah")
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(vertical = AzuraSpacing.xs)
                ) {
                    items(schools) { school ->
                        SchoolItem(school = school, onClick = { onSchoolClick(school.id) })
                    }
                }
                
                    if (globalRole == "SUPER_ADMIN") {
                        Button(
                            onClick = onAddSchoolClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = AzuraShapes.medium
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
fun SchoolItem(school: School, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = AzuraShapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        border = AssistChipDefaults.assistChipBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(AzuraSpacing.xs))
            Text(
                text = school.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = school.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (school.status == "ACTIVE") Color(0xFF2E7D32) else Color.Gray
            )
        }
    }
}
