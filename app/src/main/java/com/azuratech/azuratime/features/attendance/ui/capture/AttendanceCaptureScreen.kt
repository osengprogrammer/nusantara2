package com.azuratech.azuratime.features.attendance.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import com.azuratech.azuratime.features.attendance.ui.components.AttendanceScannerView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AttendanceCaptureScreen(
    onBarcodeScanClick: () -> Unit,
    onNavigateBack: () -> Unit,
    useBackCamera: Boolean = false,
    accountEmail: String = "admin@azuratech.com",
    viewModel: AttendanceCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Side Effects: Voice Assistant
    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collect { effect ->
            when (effect) {
                is AttendanceSideEffect.Speak -> voiceAssistant.speak(effect.message)
                AttendanceSideEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Session Lifecycle
    LaunchedEffect(accountEmail) {
        viewModel.onEvent(AttendanceCheckInUiEvent.StartScan(accountEmail))
    }

    // Sync Permission State to ViewModel
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.onEvent(AttendanceCheckInUiEvent.GrantPermission(cameraPermissionState.status.isGranted))
    }

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }

    PermissionsHandler(
        permissionState = cameraPermissionState,
        onGranted = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Layer 1: Hardware View
                AttendanceScannerView(
                    useBackCamera = currentCameraIsBack,
                    onFaceEmbeddingReady = { embedding ->
                        viewModel.onEvent(AttendanceCheckInUiEvent.FaceMatched(embedding))
                    },
                    showLivenessLabel = uiState.isScanning,
                )

                // Layer 2: Overlays
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AzuraSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (uiState.activeClassName.isEmpty()) "Scan Bebas" else "Kelas: ${uiState.activeClassName}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                            AzuraButton(text = "Flip", onClick = { currentCameraIsBack = !currentCameraIsBack }, modifier = Modifier.height(40.dp))
                            AzuraButton(
                                text = "Manual",
                                onClick = { viewModel.onEvent(AttendanceCheckInUiEvent.StartScan(accountEmail, ScanMode.Manual)) },
                                modifier = Modifier.height(40.dp),
                            )
                            AzuraButton(text = "Barcode", onClick = onBarcodeScanClick, modifier = Modifier.height(40.dp))
                        }
                    }

                    // Scan Mode Specific Overlays
                    when (uiState.scanMode) {
                        ScanMode.Manual -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                                ) {
                                    Text("Manual Entry Placeholder", color = Color.White)
                                    AzuraButton(
                                        text = "Kembali ke Wajah",
                                        onClick = { viewModel.onEvent(AttendanceCheckInUiEvent.StartScan(accountEmail, ScanMode.Face)) },
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    // Bottom Messaging
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                    ) {
                        when {
                            uiState.studentProfile != null -> {
                                AzuraCard(
                                    modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                                    title = "Presensi Berhasil",
                                    content = {
                                        Text(text = "Halo, ${uiState.studentProfile?.name}!", style = MaterialTheme.typography.bodyLarge)
                                        if (uiState.isAlreadyCheckedIn) {
                                            Text(text = "Anda sudah melakukan presensi sebelumnya.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                )
                            }
                            uiState.error != null -> {
                                AzuraCard(
                                    modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                                    title = "Presensi Gagal",
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    content = {
                                        Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodyMedium)
                                    },
                                )
                                AzuraButton(text = "Coba Lagi", onClick = { viewModel.onEvent(AttendanceCheckInUiEvent.Retry) })
                            }
                        }
                    }

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
    )
}
