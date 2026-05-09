package com.azuratech.azuratime.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.domain.model.ExportJobProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews

@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val exportJobs by viewModel.exportJobs.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Manajemen Ekspor",
        onBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.startExport("CSV") }) {
                Icon(Icons.Default.Download, contentDescription = "Mulai Ekspor")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            if (exportJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat ekspor.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(exportJobs, key = { it.jobId }) { profile ->
                        ExportJobItem(profile = profile)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportJobItem(profile: ExportJobProfile) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilePresent, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Ekspor ${profile.fileType}", style = MaterialTheme.typography.titleMedium)
                Text("Status: ${profile.status}", style = MaterialTheme.typography.bodySmall)
            }

            if (profile.syncStatus != SyncStatus.SYNCED) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Belum Sinkron",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@AzuraPreviews
@Composable
fun ExportScreenPreview() {
    MaterialTheme {
        ExportScreen(onNavigateBack = {})
    }
}
