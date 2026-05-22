package com.azuratech.azuratime.features.update.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

/**
 * 🚀 APP UPDATE DIALOG (v3.2.0-ai-native)
 * Aesthetic dialog for in-app update notifications.
 */
@Composable
fun AppUpdateDialog(
    state: AppUpdateUiState,
    onEvent: (AppUpdateUiEvent) -> Unit,
) {
    if (state.showDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(AppUpdateUiEvent.DismissDialog) },
            title = {
                Text(
                    text = "Update Tersedia! 🚀",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Versi baru telah tersedia. Silakan perbarui aplikasi Anda untuk mendapatkan fitur terbaru dan perbaikan stabilitas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (state.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Apa yang baru:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = state.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (state.downloadProgress > 0f && state.downloadProgress < 1f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = state.downloadProgress,
                            modifier = Modifier.fillMaxWidth(),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        Text(
                            text = "Mengunduh: ${(state.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                if (state.apkFile != null) {
                    Button(
                        onClick = { onEvent(AppUpdateUiEvent.InstallUpdate) },
                        shape = AzuraShapes.medium,
                    ) {
                        Text("Pasang Sekarang")
                    }
                } else {
                    Button(
                        onClick = { onEvent(AppUpdateUiEvent.DownloadUpdate) },
                        enabled = state.downloadProgress == 0f,
                        shape = AzuraShapes.medium,
                    ) {
                        Text(if (state.downloadProgress > 0f) "Mengunduh..." else "Unduh & Perbarui")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AppUpdateUiEvent.DismissDialog) }) {
                    Text("Nanti Saja")
                }
            },
        )
    }
}
