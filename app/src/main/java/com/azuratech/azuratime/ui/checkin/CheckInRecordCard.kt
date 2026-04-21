package com.azuratech.azuratime.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import java.time.format.DateTimeFormatter

/**
 * 🎫 ATTENDANCE RECEIPT CARD
 * Refactored for Pure-Class 2.0 to handle specific status coloring and class info.
 */
@Composable
fun CheckInRecordEntityCard(
    record: CheckInRecordEntity,
    onEditRequested: (CheckInRecordEntity) -> Unit = {}
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    // 🔥 DYNAMIC COLOR LOGIC
    val statusColor = when (record.status) {
        "H", "In", "Hadir" -> MaterialTheme.colorScheme.primary // Success Green -> primary
        "S" -> MaterialTheme.colorScheme.tertiary               // Sakit Yellow -> tertiary
        "I" -> MaterialTheme.colorScheme.secondary               // Izin Blue -> secondary
        "A", "Alpa" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.xs, vertical = AzuraSpacing.sm),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            // --- Row 1: Name & Status Badge ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = AzuraShapes.small
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = AzuraSpacing.sm, vertical = AzuraSpacing.xs),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            // --- Row 2: Time & Date Info ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(AzuraSpacing.xs))
                val timeToDisplay = record.checkInTime ?: record.createdAtDateTime
                Text(
                    text = "${timeToDisplay.format(timeFormatter)} • ${record.attendanceDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            // --- Row 3: Class Assignment & Edit ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                    val displayClass = if (record.className.isNullOrBlank()) "Mode Gerbang" else record.className
                    Text(
                        text = displayClass,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { onEditRequested(record) },
                    contentPadding = PaddingValues(horizontal = AzuraSpacing.sm, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = AzuraShapes.small
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(AzuraSpacing.xs))
                    Text("Koreksi", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}