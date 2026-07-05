@file:OptIn(ExperimentalPermissionsApi::class)

package com.azuratech.azuratime.features.biometric.ui.enroll

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.designsystem.CoreFaceCamera
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ml.detector.FaceAnalyzer
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BiometricScreen(
    onNavigateBack: () -> Unit,
    viewModel: BiometricEnrollmentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val enrollmentList by viewModel.enrollmentListFlow.collectAsStateWithLifecycle()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status) {
        viewModel.onEvent(
            BiometricEnrollmentUiEvent.GrantCameraPermission(
                cameraPermissionState.status is com.google.accompanist.permissions.PermissionStatus.Granted,
            ),
        )
    }

    BiometricScreenContent(
        uiState = uiState,
        enrollmentList = enrollmentList,
        onEvent = { event ->
            if (event is BiometricEnrollmentUiEvent.NavigateBack) {
                onNavigateBack()
            } else {
                viewModel.onEvent(event)
            }
        },
        onNavigateBack = onNavigateBack,
        cameraPermissionState = cameraPermissionState,
    )
}

@Composable
fun BiometricListContent(
    enrollmentList: List<com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile>,
    onDelete: (String) -> Unit,
) {
    if (enrollmentList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada biometrik terdaftar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            items(enrollmentList, key = { it.studentId }) { profile ->
                AzuraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(AzuraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StudentAvatar(photoPath = profile.photoUri, size = 56.dp)
                        Spacer(modifier = Modifier.width(AzuraSpacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.studentName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "ID: ${profile.studentId}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        IconButton(onClick = { onDelete(profile.studentId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricCaptureContent(
    onFaceCaptured: (FloatArray) -> Unit,
) {
    val analyzer = remember {
        FaceAnalyzer(
            onFaceEmbedding = { _, embedding -> onFaceCaptured(embedding) },
            onLivenessStatus = { /* Optionally show liveness status */ },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CoreFaceCamera(
            modifier = Modifier.fillMaxSize(),
            analyzer = analyzer,
            useFrontCamera = true,
        )

        Text(
            "Posisikan wajah di dalam lingkaran dan berkedip",
            style = MaterialTheme.typography.bodyMedium,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AzuraSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Izin kamera diperlukan untuk pendaftaran biometrik.")
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        AzuraButton(text = "Berikan Izin", onClick = onRequestPermission)
    }
}

@Composable
fun SuccessContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AzuraSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pendaftaran Berhasil!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        AzuraButton(text = "Selesai", onClick = onDone)
    }
}

@Composable
fun ErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AzuraSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Terjadi Kesalahan", color = MaterialTheme.colorScheme.error)
        Text(error, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        AzuraButton(text = "Coba Lagi", onClick = onRetry)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewBiometricIdle() {
    MaterialTheme {
        BiometricScreenContent(
            uiState = BiometricPreviewMocks.idle(),
            enrollmentList = emptyList(),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewBiometricCapturing() {
    MaterialTheme {
        BiometricScreenContent(
            uiState = BiometricPreviewMocks.capturing(),
            enrollmentList = emptyList(),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewBiometricSuccess() {
    MaterialTheme {
        BiometricScreenContent(
            uiState = BiometricPreviewMocks.success(),
            enrollmentList = emptyList(),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewBiometricError() {
    MaterialTheme {
        BiometricScreenContent(
            uiState = BiometricPreviewMocks.error(),
            enrollmentList = emptyList(),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun BiometricScreenContent(
    uiState: BiometricEnrollmentUiState,
    enrollmentList: List<com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile>,
    onEvent: (BiometricEnrollmentUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    cameraPermissionState: com.google.accompanist.permissions.PermissionState? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AzuraScreen(
        title = "Manajemen Biometrik",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { onEvent(BiometricEnrollmentUiEvent.SyncBiometric("")) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Sinkronkan")
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState.enrollmentStatus) {
                EnrollmentStatus.IDLE -> {
                    BiometricListContent(
                        enrollmentList = enrollmentList,
                        onDelete = { onEvent(BiometricEnrollmentUiEvent.DeleteBiometric(it)) },
                    )
                }
                EnrollmentStatus.CAPTURING -> {
                    if (uiState.cameraPermissionGranted || androidx.compose.ui.platform.LocalInspectionMode.current) {
                        BiometricCaptureContent(
                            onFaceCaptured = { embedding ->
                                onEvent(BiometricEnrollmentUiEvent.FaceCaptured(embedding))
                            },
                        )
                    } else {
                        PermissionDeniedContent(onRequestPermission = {
                            cameraPermissionState?.launchPermissionRequest()
                        })
                    }
                }
                EnrollmentStatus.PROCESSING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(AzuraSpacing.md))
                            Text("Memproses biometrik...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                EnrollmentStatus.SUCCESS -> {
                    SuccessContent(onDone = { onEvent(BiometricEnrollmentUiEvent.Retry) })
                }
                EnrollmentStatus.FAILURE -> {
                    ErrorContent(
                        error = uiState.error ?: "Gagal pendaftaran",
                        onRetry = { onEvent(BiometricEnrollmentUiEvent.Retry) },
                    )
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}
