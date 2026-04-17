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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.ui.theme.AzuraShapes
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
        "H", "In", "Hadir" -> Color(0xFF2E7D32) // Success Green
        "S" -> Color(0xFFF9A825)               // Sakit Yellow
        "I" -> Color(0xFF1565C0)               // Izin Blue
        "A", "Alpa" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Row 2: Time & Date Info ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                val timeToDisplay = record.checkInTime ?: record.createdAtDateTime
                Text(
                    text = "${timeToDisplay.format(timeFormatter)} • ${record.attendanceDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // --- Row 3: Class Assignment & Edit ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = AzuraShapes.small
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Koreksi", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}