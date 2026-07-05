package com.azuratech.azuratime.features.reporting.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.features.reporting.ui.audit.AuditLogList
import com.azuratech.azuratime.features.reporting.ui.export.ExportJobList

@Composable
fun ReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Laporan & Audit",
        onBack = onNavigateBack,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                ReportTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.onEvent(ReportUiEvent.SelectTab(tab)) },
                        text = { Text(if (tab == ReportTab.AuditLogs) "Audit Trail" else "Export") },
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    ReportTab.AuditLogs -> {
                        AuditLogList(
                            logs = uiState.auditLogs,
                            isLoading = uiState.isLoading,
                        )
                    }
                    ReportTab.ExportJobs -> {
                        ExportJobList(
                            jobs = uiState.exportJobs,
                            onStartExport = { format -> viewModel.onEvent(ReportUiEvent.StartExport(format)) },
                            onClearCompleted = { viewModel.onEvent(ReportUiEvent.ClearExportJobs) },
                        )
                    }
                }
            }

            uiState.error?.let {
                Snackbar(
                    modifier = Modifier.padding(AzuraSpacing.md),
                    action = {
                        TextButton(onClick = { viewModel.onEvent(ReportUiEvent.ClearError) }) {
                            Text("OK")
                        }
                    },
                ) {
                    Text(it)
                }
            }
        }
    }
}
