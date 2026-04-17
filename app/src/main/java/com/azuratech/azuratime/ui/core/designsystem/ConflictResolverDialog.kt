package com.azuratech.azuratime.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.data.local.AttendanceConflict
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import java.time.format.DateTimeFormatter

@Composable
fun ConflictResolverDialog(
    conflict: AttendanceConflict,
    onResolve: (useCloud: Boolean) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    AlertDialog(
        onDismissRequest = { /* Paksa user memilih, jangan biarkan dismiss klik luar */ },
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800)) },
        title = {
            Text(
                text = "Konflik Data Absensi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ditemukan perbedaan data untuk:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = conflict.local.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tanggal: ${conflict.local.attendanceDate}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- SISI LOKAL (HP) ---
                    ConflictSide(
                        title = "DI HP INI",
                        icon = Icons.Default.PhoneAndroid,
                        status = conflict.local.status,
                        time = conflict.local.checkInTime?.format(timeFormatter) ?: "--:--",
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )

                    Text("VS", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))

                    // --- SISI CLOUD (FIRESTORE) ---
                    ConflictSide(
                        title = "DI CLOUD",
                        icon = Icons.Default.Cloud,
                        status = conflict.cloud.status,
                        time = conflict.cloud.checkInTime?.format(timeFormatter) ?: "--:--",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(AzuraSpacing.md))
                Text(
                    text = "Data mana yang ingin Anda pertahankan?",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve(true) },
                shape = AzuraShapes.medium
            ) {
                Text("Gunakan Data Cloud")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onResolve(false) },
                shape = AzuraShapes.medium
            ) {
                Text("Tetap Data Lokal")
            }
        },
        shape = AzuraShapes.large
    )
}

@Composable
private fun ConflictSide(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: String,
    time: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, color = color)
        Spacer(Modifier.height(4.dp))
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = AzuraShapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
        Text(time, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
