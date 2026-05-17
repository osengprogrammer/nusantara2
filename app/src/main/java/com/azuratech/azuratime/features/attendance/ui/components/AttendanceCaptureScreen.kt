package com.azuratech.azuratime.features.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceSideEffect
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceUiState
import com.azuratech.azuratime.features.attendance.ui.components.ScannerViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun AttendanceCaptureScreen(
    onBarcodeScanClick: () -> Unit,
    onNavigateBack: () -> Unit,
    useBackCamera: Boolean = false,
    accountEmail: String = "admin@azuratech.com",
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()
    
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
        viewModel.startScannerSession(accountEmail)
    }

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }

    AttendanceCaptureContent(
        uiState = uiState,
        activeClassName = viewModel.activeClassName,
        useBackCamera = currentCameraIsBack,
        onFlipCameraClick = { currentCameraIsBack = !currentCameraIsBack },
        onSwitchToBarcodeClick = onBarcodeScanClick,
        onFaceEmbeddingReady = { embedding ->
            viewModel.processScannedBiometric(embedding)
        }
    )
}
