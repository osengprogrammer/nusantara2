package com.azuratech.azuratime.feature.audit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.api.models.AuditEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(viewModel: AuditViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audit Logs") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
            items(events) { event ->
                AuditEventCard(event)
            }
        }
    }
}

@Composable
fun AuditEventCard(event: AuditEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Action: ${event.action}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Item ID: ${event.itemId ?: "N/A"}")
            Text(text = "Status: ${event.status}")
            Text(text = "Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))}")
        }
    }
}