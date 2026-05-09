package com.azuratech.azuratime.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.ConflictResolverDialog
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

@Composable
fun DataIntegrityScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataIntegrityViewModel = hiltViewModel()
) {
    val totalFaces by viewModel.totalFaces.collectAsStateWithLifecycle(0)
    val missingAssignment by viewModel.missingAssignment.collectAsStateWithLifecycle(0)
    val brokenAssignments by viewModel.brokenAssignments.collectAsStateWithLifecycle(0)
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle(0)
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()

    var selectedConflict by remember { mutableStateOf<AttendanceConflict?>(null) }

    AzuraScreen(
        title = "Kesehatan Data",
        onBack = onNavigateBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            // --- Summary Section ---
            item {
                IntegritySummaryWidget(
                    totalFaces = totalFaces,
                    unassignedCount = missingAssignment,
                    brokenLinks = brokenAssignments,
                    unsyncedCount = unsyncedCount
                )
            }

            // --- Conflicts Section ---
            if (conflicts.isNotEmpty()) {
                item {
                    Text(
                        "Konflik Sinkronisasi (${conflicts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = AzuraSpacing.md)
                    )
                    Text(
                        "Ditemukan data ganda antara lokal dan server. Silakan pilih versi yang benar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                items(conflicts) { conflict ->
                    ConflictCard(
                        conflict = conflict,
                        onClick = { selectedConflict = conflict }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✅ Tidak ada konflik data.", color = Color.Gray)
                    }
                }
            }
        }

        selectedConflict?.let { conflict ->
            ConflictResolverDialog(
                conflict = conflict,
                onResolve = { useCloud -> 
                    viewModel.resolveConflict(conflict.conflictId, useCloud)
                    selectedConflict = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictCard(
    conflict: AttendanceConflict,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    conflict.local.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ID Siswa: ${conflict.local.studentId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
