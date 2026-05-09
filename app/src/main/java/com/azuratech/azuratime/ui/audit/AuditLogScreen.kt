package com.azuratech.azuratime.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.domain.model.AuditLogProfile
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AuditLogScreen(
    viewModel: AuditLogViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Audit Trail",
        onBack = onNavigateBack
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada log aktivitas.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(logs, key = { it.logId }) { log ->
                        AuditLogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLogProfile) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }
    val dateStr = formatter.format(Instant.ofEpochMilli(log.timestamp))

    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(log.action, style = MaterialTheme.typography.titleMedium)
                Text("User: ${log.userId}", style = MaterialTheme.typography.bodySmall)
                log.details?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@AzuraPreviews
@Composable
fun AuditLogScreenPreview() {
    MaterialTheme {
        AuditLogScreen(onNavigateBack = {})
    }
}
