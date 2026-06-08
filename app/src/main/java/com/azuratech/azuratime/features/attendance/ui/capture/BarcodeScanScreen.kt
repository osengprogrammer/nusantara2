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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.LocationProvider
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import com.azuratech.azuratime.features.attendance.ui.barcode.BarcodeScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

/**
 * 📸 BARCODE SCAN SCREEN (v3.2.1-ai-native)
 * Specialized screen for scanning student QR codes for attendance.
 * Strictly follows MVI and AI-native standards.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanScreen(
    onNavigateBack: () -> Unit,
    accountEmail: String = "", // Trigger automatic resolution
    viewModel: AttendanceCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val locationProvider = remember { LocationProvider(context) }

    // 🛡️ AI Native: Periodic Geofence Validation
    LaunchedEffect(uiState.activeSchoolId) {
        if (uiState.activeSchoolId == null) return@LaunchedEffect
        while (true) {
            locationProvider.getCurrentLocation().onSuccess { location ->
                viewModel.onEvent(AttendanceCheckInUiEvent.GeofenceValidated(location.latitude, location.longitude))
            }
            delay(10000) // 10s Re-check
        }
    }

    // UI Effects: Voice Assistant & Navigation
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AttendanceCaptureUiEffect.Speak -> voiceAssistant.speak(effect.message)
                AttendanceCaptureUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Initialize Scanner Session
    LaunchedEffect(accountEmail) {
        viewModel.onEvent(AttendanceCheckInUiEvent.StartScan(accountEmail, ScanMode.Barcode))
    }

    // Sync Permission State
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.onEvent(AttendanceCheckInUiEvent.GrantPermission(cameraPermissionState.status.isGranted))
    }

    AzuraScreen(
        title = "Presensi Barcode",
        onBack = onNavigateBack,
    ) {
        PermissionsHandler(
            permissionState = cameraPermissionState,
            onGranted = {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // 📹 Layer 1: Scanner View
                    if (uiState.isScanning && uiState.isWithinGeofence) {
                        BarcodeScanner(
                            useBackCamera = true,
                            onBarcodeDetected = { code ->
                                viewModel.onEvent(AttendanceCheckInUiEvent.BarcodeDetected(code))
                            },
                        )
                    }

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

                    // 🛠️ Layer 2: UI Overlays
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top Status Bar
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        ) {
                            Text(
                                text = if (uiState.activeClassName.isEmpty()) "Menunggu Barcode..." else "Kelas: ${uiState.activeClassName}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(AzuraSpacing.md),
                            )
                        }

                        // Bottom Feedback Area
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                        ) {
                            when {
                                uiState.studentProfile != null -> {
                                    AzuraCard(
                                        modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                                        title = "Check-in Success",
                                        content = {
                                            Text(
                                                text = "Student: ${uiState.studentProfile?.name}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            )
                                            if (uiState.isAlreadyCheckedIn) {
                                                Text(
                                                    text = "Already checked-in previously.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        },
                                    )
                                }
                                uiState.error != null -> {
                                    AzuraCard(
                                        modifier = Modifier.padding(horizontal = AzuraSpacing.lg),
                                        title = "Failed",
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        content = {
                                            Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodyMedium)
                                        },
                                    )
                                    AzuraButton(text = "Try Again", onClick = { viewModel.onEvent(AttendanceCheckInUiEvent.Retry) })
                                }
                                uiState.isLoading -> {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                                    Text("Processing...", color = Color.White)
                                }
                                else -> {
                                    if (uiState.isWithinGeofence) {
                                        Text(
                                            "Point camera at Student QR Code",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}
