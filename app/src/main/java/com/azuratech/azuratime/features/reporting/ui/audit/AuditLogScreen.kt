package com.azuratech.azuratime.features.reporting.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AuditLogScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuditLogViewModel = hiltViewModel(),
) {
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Audit Trail System",
        onBack = onNavigateBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            items(logs) { log ->
                AuditLogItem(log = log)
            }

            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.xl)) {
                        Text("Belum ada log sistem.")
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: SystemAuditTrail) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss") }
    val dateTime = remember(log.timestamp) {
        Instant.ofEpochMilli(log.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = dateTime.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "User: ${log.userId}",
                style = MaterialTheme.typography.bodySmall,
            )

            log.details?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
