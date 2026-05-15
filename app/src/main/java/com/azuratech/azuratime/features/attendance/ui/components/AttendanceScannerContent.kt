package com.azuratech.azuratime.features.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceUiState
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun AttendanceScannerContent(
    uiState: AttendanceUiState,
    activeClassName: String,
    useBackCamera: Boolean,
    onFlipCameraClick: () -> Unit,
    onSwitchToBarcodeClick: () -> Unit,
    onFaceEmbeddingReady: (FloatArray) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AttendanceScannerView(
            useBackCamera = useBackCamera,
            onFaceEmbeddingReady = onFaceEmbeddingReady,
            showLivenessLabel = uiState is AttendanceUiState.Idle
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeClassName.isEmpty()) "Scan Bebas" else "Kelas: $activeClassName",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                    AzuraButton(text = "Flip", onClick = onFlipCameraClick, modifier = Modifier.height(40.dp))
                    AzuraButton(text = "Barcode", onClick = onSwitchToBarcodeClick, modifier = Modifier.height(40.dp))
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                when (uiState) {
                    is AttendanceUiState.Success -> {
                        AzuraCard(
                            modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                            title = "Presensi Berhasil",
                            content = {
                                Text("Halo, ${uiState.name}!", style = MaterialTheme.typography.bodyLarge)
                                if (uiState.alreadyCheckedIn) {
                                    Text("Anda sudah melakukan presensi sebelumnya.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    }
                    is AttendanceUiState.Error -> {
                        AzuraCard(
                            modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                            title = "Gagal Presensi",
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            content = { Text(uiState.message, style = MaterialTheme.typography.bodyMedium) }
                        )
                    }
                    else -> {}
                }
            }

            if (uiState is AttendanceUiState.Processing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
