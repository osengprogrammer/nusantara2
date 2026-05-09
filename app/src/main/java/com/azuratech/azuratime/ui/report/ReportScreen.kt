package com.azuratech.azuratime.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.model.ReportSummaryProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val reportList by viewModel.reportList.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Dashboard Laporan",
        onBack = onNavigateBack
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            if (reportList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada laporan yang di-generate.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(reportList, key = { it.reportId }) { profile ->
                        ReportItemCard(profile = profile)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(profile: ReportSummaryProfile) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.reportName, style = MaterialTheme.typography.titleMedium)
                Text(profile.dateRange, style = MaterialTheme.typography.bodySmall)
            }

            if (profile.syncStatus != SyncStatus.SYNCED) {
                Icon(Icons.Default.CloudOff, "Belum Sinkron", tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@AzuraPreviews
@Composable
fun ReportScreenPreview() {
    MaterialTheme {
        ReportScreen(onNavigateBack = {})
    }
}
