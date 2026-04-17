package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.ui.ai.rememberVoiceAssistant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun CheckInScreen(
    useBackCamera: Boolean,
    teacherEmail: String,
    onNavigateToBarcode: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceAssistant = rememberVoiceAssistant()

    // Side Effects: Voice Assistant
    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach { effect ->
            when (effect) {
                is CheckInSideEffect.Speak -> voiceAssistant.speak(effect.message)
                is CheckInSideEffect.NavigateBack -> { /* Handle navigation */ }
            }
        }.collect()
    }

    // Session Lifecycle
    LaunchedEffect(teacherEmail) {
        viewModel.startScannerSession(teacherEmail)
    }

    var currentCameraIsBack by remember { mutableStateOf(useBackCamera) }

    CheckInContent(
        uiState = uiState,
        activeClassName = "", // Can be passed from viewModel.uiState
        useBackCamera = currentCameraIsBack,
        onFlipCamera = { currentCameraIsBack = !currentCameraIsBack },
        onSwitchToBarcode = onNavigateToBarcode,
        onFaceEmbeddingReady = { embedding ->
            viewModel.processScannedFace(embedding)
        }
    )
}
