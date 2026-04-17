package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import java.time.format.DateTimeFormatter

@Composable
fun RecentScansHeader(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Scans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { navController.navigate(Screen.CheckInRecordEntity.route) }) {
            Text("See All")
        }
    }
}

@Composable
fun DashboardCheckInItem(record: CheckInRecordEntity) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    AzuraCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        content = {
            Row(Modifier.padding(0.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.name, fontWeight = FontWeight.Bold)
                    Text("ID: ${record.faceId}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    (record.checkInTime ?: record.createdAtDateTime).format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
