package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceActionSheet(
    record: CheckInRecordEntity,
    onDismiss: () -> Unit,
    onDelete: (CheckInRecordEntity) -> Unit,
    onUpdateStatus: (CheckInRecordEntity) -> Unit,
    onShowClassCorrection: () -> Unit 
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AzuraShapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AzuraSpacing.md)
                .padding(bottom = AzuraSpacing.xl)
        ) {
            // Header Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Kelola Absensi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            Text(
                text = "Personil: ${record.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(AzuraSpacing.lg))

            // 1. Quick Status Switch
            Text("Ubah Status:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(AzuraSpacing.sm))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                val statuses = listOf("H" to "Hadir", "S" to "Sakit", "I" to "Izin", "A" to "Alpa")
                statuses.forEach { (code, label) ->
                    FilterChip(
                        selected = record.status == code,
                        onClick = { 
                            onUpdateStatus(record.copy(status = code))
                            onDismiss()
                        },
                        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        modifier = Modifier.weight(1f),
                        shape = AzuraShapes.small
                    )
                }
            }

            Spacer(Modifier.height(AzuraSpacing.lg))

            // 2. 🔥 THE SAVIOR BUTTON: Koreksi Kelas
            Text("Salah Sesi/Kelas?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(AzuraSpacing.sm))
            
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onShowClassCorrection() 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AzuraShapes.medium
            ) {
                Icon(Icons.Default.SwapHoriz, null)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pindahkan Sesi Kelas", fontWeight = FontWeight.Bold)
                    Text("Saat ini: ${record.className ?: "Umum"}", style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.ChevronRight, null)
            }

            Spacer(Modifier.height(AzuraSpacing.xl))

            // 3. Delete Action
            Button(
                onClick = { onDelete(record); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = AzuraShapes.medium
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Hapus Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}