package com.azuratech.azuratime.features.attendance.ui.barcode

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// 🔥 Custom Components & Utils
import com.azuratech.azuratime.features.attendance.ui.components.ScannerViewModel
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceUiState
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceSideEffect
import com.azuratech.azuratime.features.attendance.ui.components.MatchResultLabel
import com.azuratech.azuratime.features.attendance.ui.components.StatusLabel
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.features.ai.ui.rememberVoiceAssistant
import androidx.compose.foundation.layout.Column
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach


@Composable
fun BarcodeScreen(
    accountEmail: String,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val voiceAssistant = rememberVoiceAssistant()
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    var currentCameraIsBack by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.sideEffectFlow.onEach { effect ->
            when (effect) {
                is AttendanceSideEffect.Speak -> voiceAssistant.speak(effect.message)
                else -> {}
            }
        }.collect()
    }

    LaunchedEffect(accountEmail) {
        viewModel.startScannerSession(accountEmail)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BarcodeScanner(
            useBackCamera = currentCameraIsBack,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) { barcodeValue ->
            if (uiState !is AttendanceUiState.Processing) {
                viewModel.processScannedBarcode(barcodeValue)
            }
        }

        HeaderOverlayBarcode(
            activeClass = viewModel.activeClassName,
            onFlipCamera = { currentCameraIsBack = !currentCameraIsBack }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            when (val state = uiState) {
                is AttendanceUiState.Success -> {
                    MatchResultLabel(
                        name = state.name,
                        isAlreadyIn = state.alreadyCheckedIn,
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                }
                is AttendanceUiState.Error -> {
                    StatusLabel(text = "⛔ ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }

        if (uiState is AttendanceUiState.Processing) {
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f), 
            shape = AzuraShapes.medium,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    "AZURA TIME: BARCODE", 
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                )
                val display = if (activeClass.isBlank()) "SCAN BEBAS" else activeClass.uppercase()
                Text(
                    display, 
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                )
            }
        }
        
        FilledIconButton(
            onClick = onFlipCamera,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
        }
    }
}