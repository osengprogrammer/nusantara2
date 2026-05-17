package com.azuratech.azuratime.features.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureUiEvent
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureViewModel
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceSideEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

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
        viewModel.onEvent(AttendanceCaptureUiEvent.StartScan(accountEmail))
    }

    // Sync Permission State to ViewModel
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.onEvent(AttendanceCaptureUiEvent.GrantPermission(cameraPermissionState.status.isGranted))
    }

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }

    com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler(
        permissionState = cameraPermissionState,
        onGranted = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Layer 1: Hardware View
                AttendanceScannerView(
                    useBackCamera = currentCameraIsBack,
                    onFaceEmbeddingReady = { embedding ->
                        viewModel.onEvent(AttendanceCaptureUiEvent.FaceDetected(embedding))
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
                            AzuraButton(text = "Barcode", onClick = onBarcodeScanClick, modifier = Modifier.height(40.dp))
                        }
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
                                        if (uiState.studentProfile?.alreadyCheckedIn == true) {
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

@Preview(showBackground = true)
@Composable
private fun PreviewScanning() {
    AzuraTheme {
        // Simple mock view since we can't render the camera in preview easily
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            Text("Scanning Preview Mode...", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSuccess() {
    AzuraTheme {
        val state = com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCapturePreviewMocks.success()
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            AzuraCard(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                title = "Presensi Berhasil",
            ) {
                Text(text = "Halo, ${state.studentProfile?.name}!", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewError() {
    AzuraTheme {
        val state = com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCapturePreviewMocks.error()
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            AzuraCard(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                title = "Presensi Gagal",
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(text = state.error ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
