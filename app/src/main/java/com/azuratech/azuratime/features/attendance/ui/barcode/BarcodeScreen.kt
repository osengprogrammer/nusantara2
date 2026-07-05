package com.azuratech.azuratime.features.attendance.ui.barcode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureViewModel
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCheckInUiEvent
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureUiEffect
import com.azuratech.azuratime.features.attendance.ui.capture.ScanMode
import com.azuratech.azuratime.features.attendance.ui.components.MatchResultLabel
import com.azuratech.azuratime.features.attendance.ui.components.StatusLabel
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import kotlinx.coroutines.flow.collect

@Composable
fun BarcodeScreen(
    accountEmail: String,
    viewModel: AttendanceCaptureViewModel = hiltViewModel(),
) {
    val voiceAssistant = rememberVoiceAssistant()
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var currentCameraIsBack by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AttendanceCaptureUiEffect.Speak -> voiceAssistant.speak(effect.message)
                else -> {}
            }
        }
    }

    LaunchedEffect(accountEmail) {
        viewModel.onEvent(AttendanceCheckInUiEvent.StartScan(accountEmail, ScanMode.Barcode))
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BarcodeScanner(
            useBackCamera = currentCameraIsBack,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize(),
        ) { barcodeValue ->
            if (!uiState.isLoading) {
                viewModel.onEvent(AttendanceCheckInUiEvent.BarcodeDetected(barcodeValue))
            }
        }

        HeaderOverlayBarcode(
            activeClass = uiState.activeClassName,
            onFlipCamera = { currentCameraIsBack = !currentCameraIsBack },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            uiState.studentProfile?.let { profile ->
                MatchResultLabel(
                    name = profile.name,
                    isAlreadyIn = uiState.isAlreadyCheckedIn,
                    primaryColor = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.error?.let { error ->
                StatusLabel(text = "⛔ $error", color = MaterialTheme.colorScheme.error)
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun HeaderOverlayBarcode(activeClass: String, onFlipCamera: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(AzuraSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = AzuraShapes.medium,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    "AZURA TIME: BARCODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                    ),
                )
                val display = if (activeClass.isBlank()) "GENERAL SCAN" else activeClass.uppercase()
                Text(
                    display,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                )
            }
        }

        FilledIconButton(
            onClick = onFlipCamera,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.7f),
            ),
        ) {
            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
        }
    }
}
