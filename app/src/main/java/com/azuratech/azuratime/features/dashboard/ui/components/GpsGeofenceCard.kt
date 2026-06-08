package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.school.data.local.GpsGeofenceEntity

/**
 * 📍 GPS GEOFENCE CARD (v3.2.1-ai-native)
 * Displays the current status of the school's GPS geofence on the Dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsGeofenceCard(
    geofence: GpsGeofenceEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = geofence?.isActive ?: false
    val statusText = if (isActive) "Active" else "Inactive"
    val statusColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon = if (isActive) Icons.Default.GpsFixed else Icons.Default.GpsOff

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GPS Geofence: $statusText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
                if (isActive && geofence != null) {
                    Text(
                        text = "Radius: ${geofence.radiusMeters}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Restrict attendance by location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onClick) {
                Text("Manage")
            }
        }
    }
}
