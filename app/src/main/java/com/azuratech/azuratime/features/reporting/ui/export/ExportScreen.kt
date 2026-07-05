package com.azuratech.azuratime.features.reporting.ui.export

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.reporting.ui.ReportViewModel
import com.azuratech.azuratime.features.reporting.ui.ReportUiEvent

@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Export Data",
        onBack = onNavigateBack,
    ) {
        ExportJobList(
            jobs = uiState.exportJobs,
            onStartExport = { format -> viewModel.onEvent(ReportUiEvent.StartExport(format)) },
            onClearCompleted = { viewModel.onEvent(ReportUiEvent.ClearExportJobs) },
        )
    }
}

@Composable
fun ExportJobList(
    jobs: List<ExportJobProfile>,
    onStartExport: (String) -> Unit,
    onClearCompleted: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            Button(onClick = { onStartExport("CSV") }, modifier = Modifier.weight(1f)) {
                Text("Export CSV")
            }
            Button(onClick = { onStartExport("EXCEL") }, modifier = Modifier.weight(1f)) {
                Text("Export Excel")
            }
        }

        Spacer(modifier = Modifier.height(AzuraSpacing.md))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Riwayat Export", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClearCompleted) {
                Text("Bersihkan")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            items(jobs) { job ->
                ExportJobItem(job = job)
            }

            if (jobs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.xl), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat export.")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportJobItem(job: ExportJobProfile) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Job ID: ${job.jobId.take(8)}...", style = MaterialTheme.typography.labelSmall)
                Text(text = "Format: ${job.fileType}", style = MaterialTheme.typography.bodyMedium)
            }

            Badge(
                containerColor = when (job.status) {
                    "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
                    "FAILED" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
            ) {
                Text(job.status)
            }
        }
    }
}
