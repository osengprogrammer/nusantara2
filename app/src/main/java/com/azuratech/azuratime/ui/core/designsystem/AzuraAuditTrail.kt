package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun AzuraAuditTrail(
    createdAt: Long,
    createdBy: String?,
    lastUpdated: Long,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(
                text = "LOG AUDIT & HISTORY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(AzuraSpacing.sm))

            AuditRow(
                label = "Dibuat oleh",
                value = createdBy ?: "Sistem",
                date = dateFormatter.format(java.util.Date(createdAt)),
                icon = Icons.Filled.AddModerator
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AzuraSpacing.sm),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            AuditRow(
                label = "Update Terakhir",
                value = "Sesi Saat Ini",
                date = dateFormatter.format(java.util.Date(lastUpdated)),
                icon = Icons.Filled.HistoryEdu
            )
        }
    }
}

@Composable
private fun AuditRow(label: String, value: String, date: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
