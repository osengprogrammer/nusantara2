package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceActionSheet(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onDelete: (AttendanceRecord) -> Unit,
    onUpdateStatus: (AttendanceRecord, AttendanceStatus) -> Unit,
    onShowClassCorrection: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AzuraShapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AzuraSpacing.md)
                .padding(bottom = AzuraSpacing.xl),
        ) {
            // Header Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Kelola Absensi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Personil: ${record.studentName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(AzuraSpacing.lg))

            // 1. Quick Status Switch
            Text("Ubah Status:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(AzuraSpacing.sm))

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                AttendanceStatus.values().forEach { status ->
                    val label = when (status) {
                        AttendanceStatus.PRESENT -> "Hadir"
                        AttendanceStatus.LATE -> "Terlambat"
                        AttendanceStatus.SICK -> "Sakit"
                        AttendanceStatus.EXCUSED -> "Izin"
                        AttendanceStatus.ABSENT -> "Alpa"
                    }
                    FilterChip(
                        selected = record.status == status,
                        onClick = {
                            onUpdateStatus(record, status)
                            onDismiss()
                        },
                        label = { Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        shape = AzuraShapes.small,
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
                shape = AzuraShapes.medium,
            ) {
                Icon(Icons.Default.SwapHoriz, null)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pindahkan Sesi Kelas", fontWeight = FontWeight.Bold)
                    Text("Saat ini: ${record.className.ifBlank { "Umum" }}", style = MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.ChevronRight, null)
            }

            Spacer(Modifier.height(AzuraSpacing.xl))

            // 3. Delete Action
            Button(
                onClick = {
                    onDelete(record)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = AzuraShapes.medium,
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Hapus Record", fontWeight = FontWeight.Bold)
            }
        }
    }
}
