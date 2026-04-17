package com.azuratech.azuratime.ui.report.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import java.time.format.DateTimeFormatter

/**
 * 📜 HISTORY LIST VIEW (PURE-CLASS 2.0)
 * Menampilkan log mentah dari database untuk audit detail.
 */
@Composable
fun HistoryListView(
    historyData: List<CheckInRecordEntity>,
    searchQuery: String,
    classMap: Map<String, String>
) {
    // Optimization: Only re-filter when data or query changes
    val filteredHistory = remember(historyData, searchQuery) {
        if (searchQuery.isBlank()) {
            historyData
        } else {
            historyData.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (filteredHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (searchQuery.isEmpty()) "Belum ada riwayat absensi." else "Pencarian tidak ditemukan.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp), // Space for navigation
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            itemsIndexed(filteredHistory, key = { _, record -> record.id }) { _, record ->
                HistoryLogCard(record = record, classMap = classMap)
            }
        }
    }
}

@Composable
private fun HistoryLogCard(record: CheckInRecordEntity, classMap: Map<String, String>) {
    val formatterDate = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss") // Added seconds for precision

    // Pure-Class mapping logic
    val assignedClassName = record.className ?: classMap[record.classId] ?: "General Scan"

    val statusColor = when (record.status) {
        "H", "In", "Hadir" -> Color(0xFF2E7D32) // Success Green
        "S" -> Color(0xFFF9A825)               // Sakit Yellow
        "I" -> Color(0xFF1565C0)               // Izin Blue
        "Out" -> Color(0xFFE65100)             // Orange
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TIME BLOCK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Text(
                    text = (record.checkInTime ?: record.createdAtDateTime).format(formatterTime),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.attendanceDate.format(formatterDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // INFO BLOCK
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = assignedClassName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                // 🔥 NEW: Teacher / Device ID Traceability
                Text(
                    text = "Operator: ${record.userId.take(8)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.6f)
                )
            }

            // STATUS BADGE
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = AzuraShapes.small,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = record.status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}