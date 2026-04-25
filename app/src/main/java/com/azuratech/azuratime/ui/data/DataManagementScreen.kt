package com.azuratech.azuratime.ui.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector // 🔥 FIXED: Added missing import
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.add.RegisterState

// 🔥 ViewModels & Utils
import com.azuratech.azuratime.ui.add.RegisterViewModel
import com.azuratech.azuraengine.model.ProcessResult

// 🔥 Azura Design System
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

/**
 * 🛠️ DATA MANAGEMENT SCREEN (PURE-CLASS 2.0)
 * Hub pusat untuk impor/ekspor CSV dan manajemen data master.
 */
@Composable
fun DataManagementScreen(
    initialDataType: String = "FACES",
    onNavigateBack: () -> Unit,
    onNavigateToClassList: () -> Unit, // 🔥 Link langsung ke Manajemen Kelas
    registerViewModel: RegisterViewModel
) {
    val context = LocalContext.current
    val state by registerViewModel.state.collectAsState()
    var selectedType by remember { mutableStateOf(initialDataType) }

    val screenTitle = when(selectedType) {
        "FACES" -> "Master Personil"
        "CLASS_MASTER" -> "Master Kelas"
        "ASSIGNMENT" -> "Penempatan Kelas"
        else -> "Data Center"
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { registerViewModel.processCsvFile(it, selectedType) }
    }

    AzuraScreen(title = screenTitle, onBack = onNavigateBack) {
        // 🔥 Replaced standard padding with top padding to prevent double horizontal padding from AzuraScreen
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.lg),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- 1. KATEGORI GRID (LEAN VERSION) ---
            item {
                Text("Pilih Kategori Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataTypeGridItem("Personil", Icons.Default.People, selectedType == "FACES", Modifier.weight(1f)) { 
                        selectedType = "FACES"; registerViewModel.resetState() 
                    }
                    DataTypeGridItem("Kelas", Icons.Default.School, selectedType == "CLASS_MASTER", Modifier.weight(1f)) { 
                        selectedType = "CLASS_MASTER"; registerViewModel.resetState() 
                    }
                    DataTypeGridItem("Penempatan", Icons.Default.AssignmentInd, selectedType == "ASSIGNMENT", Modifier.weight(1f)) { 
                        selectedType = "ASSIGNMENT"; registerViewModel.resetState() 
                    }
                }
            }

            // --- 2. ACTION CARD (CSV OPS) ---
            item {
                DataCategoryCard(
                    title = "Operasi Massal CSV",
                    description = "Impor data $screenTitle via file CSV. Pastikan format kolom sudah sesuai template.",
                    icon = Icons.Default.CloudSync,
                    onImport = { csvPickerLauncher.launch("text/comma-separated-values") },
                    onExport = { registerViewModel.exportMasterData(context, selectedType) },
                    onDownloadTemplate = { registerViewModel.downloadCsvTemplate(context, selectedType) }
                )
            }

            // --- 3. KELOLA MANUAL (Hanya untuk Kelas) ---
            if (selectedType == "CLASS_MASTER") {
                item {
                    ManualManagementCard(
                        title = "Kelola Manual Kelas",
                        description = "Tambah atau edit nama kelas satu per satu tanpa file CSV.",
                        onClick = onNavigateToClassList
                    )
                }
            }

            // --- 4. PROGRESS BAR ---
            if (state.isProcessing) {
                item {
                    ProcessingIndicator(state.progress, state.status)
                }
            }

            // --- 5. LOGS ---
            if (state.results.isNotEmpty() && !state.isProcessing) {
                item { ProcessStats(state) }
                // 🔥 Avoided hardcoded key requirement in case ID is missing from result
                items(state.results) { result -> LogItemRow(result) }
            }
        }
    }
}

@Composable
fun ProcessingIndicator(progress: Float, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text("Sedang Memproses...", style = MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(
                progress = { progress }, 
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                strokeCap = StrokeCap.Round
            )
            Text(status, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// 🔥 ADDED: Defined missing ProcessStats function
@Composable
fun ProcessStats(state: RegisterState) {
    val total = state.results.size
    val success = state.results.count { it.status.contains("Registered", ignoreCase = true) || it.status.contains("Updated", ignoreCase = true) }
    val failed = total - success

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBadge(label = "Total", count = total, color = MaterialTheme.colorScheme.primary)
        StatBadge(label = "Sukses", count = success, color = Color(0xFF4CAF50))
        if (failed > 0) {
            StatBadge(label = "Gagal", count = failed, color = MaterialTheme.colorScheme.error)
        }
    }
}

// 🔥 KOMPONEN BARU: Card untuk Kelola Manual
@OptIn(ExperimentalMaterial3Api::class) // Added for onClick in Card
@Composable
fun ManualManagementCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Buka", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun DataCategoryCard(title: String, description: String, icon: ImageVector, onImport: () -> Unit, onExport: () -> Unit, onDownloadTemplate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)
            OutlinedButton(onClick = onDownloadTemplate, modifier = Modifier.fillMaxWidth().height(40.dp), shape = AzuraShapes.medium, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Unduh Template CSV")
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onImport, modifier = Modifier.weight(1f).height(40.dp), shape = AzuraShapes.medium) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Import")
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f).height(40.dp), shape = AzuraShapes.medium) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Export")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Added for onClick in Surface
@Composable
fun DataTypeGridItem(title: String, icon: ImageVector, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.height(70.dp), onClick = onClick, shape = AzuraShapes.medium, color = bgColor, tonalElevation = if (isSelected) 4.dp else 0.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LogItemRow(result: ProcessResult) {
    val isSuccess = result.status.contains("Registered") || result.status.contains("Updated")
    val color = if (isSuccess) Color(0xFF4CAF50) else if (result.status.contains("Duplicate")) Color(0xFFFFC107) else Color.Red
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = AzuraShapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(AzuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                // Using safe call in case faceId is null/missing in some custom results
                Text("ID: ${result.faceId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(result.status, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, color)) {
        Text(text = "$label: $count", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}