package com.azuratech.azuratime.features.attendance.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.components.rememberVoiceAssistant
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
    accountEmail: String = "",
    viewModel: AttendanceCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // UI Effects: Voice Assistant
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AttendanceCaptureUiEffect.Speak -> voiceAssistant.speak(effect.message)
                AttendanceCaptureUiEffect.NavigateBack -> onNavigateBack()
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
                        if (uiState.isWithinGeofence) {
                            viewModel.onEvent(AttendanceCheckInUiEvent.FaceMatched(embedding))
                        }
                    },
                    showLivenessLabel = uiState.isScanning && uiState.isWithinGeofence,
                )

                // 🛡️ Layer 1.5: Geofence Security Overlay
                if (!uiState.isWithinGeofence) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                            .padding(AzuraSpacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(80.dp),
                            )
                            Text(
                                text = "OUT OF LOCATION",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            )
                            Text(
                                text = "Attendance features are restricted. You must be within the school premises to record attendance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(AzuraSpacing.lg))
                            AzuraButton(
                                text = "Go Back",
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            )
                        }
                    }
                }

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

                        if (uiState.isWithinGeofence) {
                            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                                AzuraButton(
                                    text = "Flip",
                                    onClick = { currentCameraIsBack = !currentCameraIsBack },
                                    modifier = Modifier.height(40.dp),
                                )
                                AzuraButton(
                                    text = "Manual",
                                    onClick = {
                                        viewModel.onEvent(
                                            AttendanceCheckInUiEvent.StartScan(
                                                accountEmail,
                                                ScanMode.Manual,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.height(40.dp),
                                )
                                AzuraButton(
                                    text = "Barcode",
                                    onClick = onBarcodeScanClick,
                                    modifier = Modifier.height(40.dp),
                                )
                            }
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
                                    title = "Attendance Success",
                                    content = {
                                        Text(text = "Hello, ${uiState.studentProfile?.name}!", style = MaterialTheme.typography.bodyLarge)
                                        if (uiState.isAlreadyCheckedIn) {
                                            Text(text = "You have already checked-in previously.", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                )
                            }
                            uiState.error != null -> {
                                AzuraCard(
                                    modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                                    title = "Attendance Failed",
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    content = {
                                        Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodyMedium)
                                    },
                                )
                                AzuraButton(text = "Try Again", onClick = { viewModel.onEvent(AttendanceCheckInUiEvent.Retry) })
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
