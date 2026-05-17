package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import java.time.format.DateTimeFormatter

@Composable
fun RecentScansHeader(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Recent Scans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { navController.navigate(Screen.AttendanceHistory.route) }) {
            Text("See All")
        }
    }
}

@Composable
fun DashboardAttendanceItem(record: AttendanceRecordEntity) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateTime = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(record.timestamp),
        java.time.ZoneId.systemDefault(),
    )
    AzuraCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        content = {
            Row(Modifier.padding(0.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.name, fontWeight = FontWeight.Bold)
                    Text("ID: ${record.studentId}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    dateTime.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
