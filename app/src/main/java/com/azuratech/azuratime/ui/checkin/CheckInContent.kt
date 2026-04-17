package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraTheme


@Composable
fun CheckInContent(
    uiState: CheckInUiState,
    activeClassName: String,
    useBackCamera: Boolean,
    onFlipCamera: () -> Unit,
    onSwitchToBarcode: () -> Unit,
    onFaceEmbeddingReady: (FloatArray) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Layer 1: Hardware View (Passed as a component we maintain here or provided by Screen)
        RealtimeScannerView(
            useBackCamera = useBackCamera,
            onFaceEmbeddingReady = onFaceEmbeddingReady,
            showLivenessLabel = uiState is CheckInUiState.Idle
        )

        // Layer 2: Design System Overlays
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AzuraSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeClassName.isEmpty()) "Scan Bebas" else "Kelas: $activeClassName",
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                    AzuraButton(
                        text = "Flip",
                        onClick = onFlipCamera,
                        modifier = Modifier.height(40.dp)
                    )
                    AzuraButton(
                        text = "Barcode",
                        onClick = onSwitchToBarcode,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            // Bottom Status Messaging
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                when (uiState) {
                    is CheckInUiState.Success -> {
                        AzuraCard(
                            modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                            title = "Check-In Berhasil",
                            content = {
                                Text(
                                    text = "Halo, ${uiState.name}!",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                                )
                                if (uiState.alreadyCheckedIn) {
                                    Text(
                                        text = "Anda sudah melakukan presensi sebelumnya.",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )
                    }
                    is CheckInUiState.Error -> {
                        AzuraCard(
                            modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                            title = "Gagal Check-In",
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                            ),
                            content = {
                                Text(
                                    text = uiState.message,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                    else -> {}
                }
            }

            if (uiState is CheckInUiState.Processing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}


@AzuraPreviews
@Composable
fun CheckInContentSuccessPreview() {
    AzuraTheme {
        Surface {
            CheckInContent(
                uiState = CheckInUiState.Success(name = "Budi Santoso", alreadyCheckedIn = false),
                activeClassName = "Kelas 10A",
                useBackCamera = false,
                onFlipCamera = {},
                onSwitchToBarcode = {},
                onFaceEmbeddingReady = {}
            )
        }
    }
}

@AzuraPreviews
@Composable
fun CheckInContentProcessingPreview() {
    AzuraTheme {
        Surface {
            CheckInContent(
                uiState = CheckInUiState.Processing,
                activeClassName = "Kelas 10A",
                useBackCamera = false,
                onFlipCamera = {},
                onSwitchToBarcode = {},
                onFaceEmbeddingReady = {}
            )
        }
    }
}
