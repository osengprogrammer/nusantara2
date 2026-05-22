package com.azuratech.azuratime.features.student.ui.roster

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRosterBarcodeScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentRosterBarcodeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var selectedStudentIdForPreview by remember { mutableStateOf<String?>(null) }
    var selectedStudentNameForPreview by remember { mutableStateOf("") }
    var selectedClassIdForPreview by remember { mutableStateOf("") }

    AzuraScreen(
        title = "Cetak Barcode",
        onBack = onNavigateBack,
        actions = {
            if (uiState.selectedStudentIds.isNotEmpty()) {
                IconButton(onClick = { viewModel.onEvent(StudentRosterBarcodeUiEvent.DeselectAll) }) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Deselect All")
                }
            } else {
                IconButton(onClick = { viewModel.onEvent(StudentRosterBarcodeUiEvent.SelectAll) }) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Summary Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(AzuraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Pilih siswa untuk dicetak", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${uiState.selectedStudentIds.size} dipilih dari ${uiState.students.size} siswa",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    if (uiState.selectedStudentIds.isNotEmpty()) {
                        Button(
                            onClick = { /* Export PDF logic here */ },
                            shape = AzuraShapes.medium,
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Print PDF")
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                ) {
                    items(uiState.students, key = { it.profile.studentId }) { item ->
                        val isSelected = uiState.selectedStudentIds.contains(item.profile.studentId)

                        Card(
                            onClick = {
                                selectedStudentIdForPreview = item.profile.studentId
                                selectedStudentNameForPreview = item.profile.name
                                selectedClassIdForPreview = item.profile.classId?.takeIf { it.isNotBlank() } ?: "UNASSIGNED"
                            },
                            shape = AzuraShapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(AzuraSpacing.md).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.onEvent(StudentRosterBarcodeUiEvent.ToggleSelection(item.profile.studentId)) },
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.profile.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "ID: ${item.profile.studentId} | ${item.assignedClassNames.ifBlank { "UNASSIGNED" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                    )
                                }
                                IconButton(onClick = {
                                    selectedStudentIdForPreview = item.profile.studentId
                                    selectedStudentNameForPreview = item.profile.name
                                    selectedClassIdForPreview = item.profile.classId?.takeIf { it.isNotBlank() } ?: "UNASSIGNED"
                                }) {
                                    Icon(Icons.Default.QrCode, contentDescription = "Preview QR", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Preview Dialog
    if (selectedStudentIdForPreview != null) {
        val qrContent = "${uiState.schoolId}|$selectedClassIdForPreview|$selectedStudentIdForPreview"
        val bitmap = generateQrCode(qrContent)

        AlertDialog(
            onDismissRequest = { selectedStudentIdForPreview = null },
            title = { Text("Preview QR Code") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(selectedStudentNameForPreview, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Class ID : $selectedClassIdForPreview", style = MaterialTheme.typography.labelSmall)

                    Spacer(Modifier.height(16.dp))

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(qrContent, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        if (bitmap != null) {
                            shareQrCode(context, bitmap, selectedStudentNameForPreview)
                        }
                    }) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                    TextButton(onClick = { selectedStudentIdForPreview = null }) {
                        Text("Tutup")
                    }
                }
            },
        )
    }
}

private fun shareQrCode(context: Context, bitmap: Bitmap, studentName: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "student_qr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Barcode AzuraTime - $studentName")
            putExtra(Intent.EXTRA_TEXT, "Barcode untuk siswa: $studentName")
            type = "image/png"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Barcode"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateQrCode(text: String): Bitmap? {
    if (text.isEmpty()) return null
    return try {
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
