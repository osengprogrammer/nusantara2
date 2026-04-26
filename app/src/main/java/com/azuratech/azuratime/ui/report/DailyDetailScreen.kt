package com.azuratech.azuratime.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// 🔥 Database Entities & ViewModels
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.checkin.CheckInViewModel
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.user.UserManagementViewModel

// 🔥 Azura Design System
import com.azuratech.azuratime.ui.core.designsystem.AttendanceActionSheet
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    faceId: String,
    studentName: String,
    dateString: String,
    viewModel: DailyDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToManual: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // UI Local State
    var selectedRecordForAction by remember { mutableStateOf<CheckInRecordEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showClassCorrectionDialog by remember { mutableStateOf<CheckInRecordEntity?>(null) }

    when (val state = uiState) {
        is DailyDetailUiState.Loading -> {
            AzuraScreen(title = "Detail: $studentName", onBack = onBack) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is DailyDetailUiState.Success -> {
            val data = state.data
            
            AzuraScreen(
                title = "Detail: $studentName",
                onBack = onBack
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.md)) {
                    
                    // Correction Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(AzuraSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Log Koreksi Manual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Tambahkan log jika scanner terlewat.", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { onNavigateToManual(faceId, dateString) }) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Manual Log", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AzuraSpacing.md))

                    // Attendance Records List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(items = data.filteredLogs, key = { it.id.toString() + it.timestamp.toString() }) { record ->
                            LogItemRow(
                                record = record,
                                onClick = {
                                    selectedRecordForAction = record
                                    showSheet = true
                                }
                            )
                        }
                    }
                }

                // Action Sheet & Dialogs
                if (showSheet && selectedRecordForAction != null) {
                    AttendanceActionSheet(
                        record = selectedRecordForAction!!,
                        onDismiss = { showSheet = false },
                        onDelete = { record -> viewModel.deleteRecord(record) },
                        onUpdateStatus = { updatedRecord -> viewModel.updateRecord(updatedRecord) },
                        onShowClassCorrection = { showClassCorrectionDialog = selectedRecordForAction }
                    )
                }

                showClassCorrectionDialog?.let { recordToCorrect ->
                    val filteredClasses = if (data.isAdmin) data.globalClasses else data.globalClasses.filter { it.id in data.assignedIds }
                    var selectedClass by remember { mutableStateOf<ClassModel?>(null) }

                    AlertDialog(
                        onDismissRequest = { showClassCorrectionDialog = null },
                        title = { Text("Pindah Kelas") },
                        text = {
                            LazyColumn {
                                items(filteredClasses) { cls ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedClass = cls }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedClass?.id == cls.id,
                                            onClick = { selectedClass = cls }
                                        )
                                        Text(text = cls.name, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                enabled = selectedClass != null,
                                onClick = {
                                    selectedClass?.let { viewModel.updateRecordClass(recordToCorrect, it) }
                                    showClassCorrectionDialog = null
                                }
                            ) { Text("Simpan") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClassCorrectionDialog = null }) { Text("Batal") }
                        }
                    )
                }
            }
        }
        is DailyDetailUiState.Error -> {
            AzuraScreen(title = "Detail: $studentName", onBack = onBack) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// 🔥 ADDED: The missing LogItemRow composable
@Composable
fun LogItemRow(record: CheckInRecordEntity, onClick: () -> Unit) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeString = record.checkInTime?.format(timeFormatter) ?: "--:--"
    
    val (statusLabel, statusColor) = when (record.status) {
        "H" -> "Hadir" to MaterialTheme.colorScheme.primary
        "S" -> "Sakit" to MaterialTheme.colorScheme.tertiary
        "I" -> "Izin" to MaterialTheme.colorScheme.secondary
        "A" -> "Alpa" to MaterialTheme.colorScheme.error
        else -> "Unknown" to MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = AzuraShapes.small
            ) {
                Text(
                    text = timeString,
                    modifier = Modifier.padding(horizontal = AzuraSpacing.sm, vertical = AzuraSpacing.xs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.className ?: "General Scan", 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Oleh: ${record.userId}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = AzuraShapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = AzuraSpacing.sm, vertical = AzuraSpacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}