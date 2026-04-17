package com.azuratech.azuratime.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun IntegritySummaryWidget(
    totalFaces: Int,
    unassignedCount: Int,
    brokenLinks: Int,
    unsyncedCount: Int,
    modifier: Modifier = Modifier
) {
    val hasIssues = unassignedCount > 0 || brokenLinks > 0 || unsyncedCount > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (hasIssues)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasIssues) Icons.Default.GppMaybe else Icons.Default.GppGood,
                        contentDescription = null,
                        tint = if (hasIssues) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Kesehatan Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (unsyncedCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("$unsyncedCount Pending", modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IntegrityStatItem(
                    label = "Total Siswa",
                    value = totalFaces.toString(),
                    icon = Icons.Default.People
                )
                IntegrityStatItem(
                    label = "Tanpa Kelas",
                    value = unassignedCount.toString(),
                    icon = Icons.Default.PersonOff,
                    isWarning = unassignedCount > 0
                )
                IntegrityStatItem(
                    label = "Link Rusak",
                    value = brokenLinks.toString(),
                    icon = Icons.Default.LinkOff,
                    isWarning = brokenLinks > 0
                )
            }
        }
    }
}

@Composable
private fun IntegrityStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    isWarning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
