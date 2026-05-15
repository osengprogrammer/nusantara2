package com.azuratech.azuratime.features.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceUiState
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceSideEffect
import com.azuratech.azuratime.features.attendance.ui.components.ScannerViewModel
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun AttendanceCaptureScreen(
    useBackCamera: Boolean,
    teacherEmail: String,
    onBarcodeScanClick: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()
    
    // 🔥 Get email from ViewModel if not passed (for NavGraph compat)
    val resolvedEmail = remember(teacherEmail) { 
        teacherEmail.ifBlank { "admin@azuratech.com" } 
    }

    // Side Effects: Voice Assistant
    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.collect { effect ->
            when (effect) {
                is AttendanceSideEffect.Speak -> voiceAssistant.speak(effect.message)
                AttendanceSideEffect.NavigateBack -> { /* Handle navigation */ }
            }
        }
    }

    // Session Lifecycle
    LaunchedEffect(resolvedEmail) {
        viewModel.startScannerSession(resolvedEmail)
    }

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }

    AttendanceScannerContent(
        uiState = uiState,
        activeClassName = viewModel.activeClassName,
        useBackCamera = currentCameraIsBack,
        onFlipCameraClick = { currentCameraIsBack = !currentCameraIsBack },
        onSwitchToBarcodeClick = onBarcodeScanClick,
        onFaceEmbeddingReady = { embedding ->
            viewModel.processScannedFace(embedding)
        }
    )
}
