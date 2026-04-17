package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.checkin.LivenessLabel


/**
 * A stateful component that encapsulates the high-frequency state updates
 * from the camera's liveness detection, preventing the main screen from
 * recomposing unnecessarily.
 */
@Composable
fun RealtimeScannerView(
    useBackCamera: Boolean,
    onFaceEmbeddingReady: (embedding: FloatArray) -> Unit,
    modifier: Modifier = Modifier,
    showLivenessLabel: Boolean
) {
    var livenessInstruction by remember { mutableStateOf("Mencari Wajah...") }

    Box(modifier = modifier) {
        FaceScanner(
            useBackCamera = useBackCamera,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize(),
            onLivenessStatus = { status -> livenessInstruction = status }
        ) { _, embedding ->
            // The onFaceDetected lambda from FaceScanner
            onFaceEmbeddingReady(embedding)
        }

        // The overlay for real-time feedback (liveness)
        if (showLivenessLabel) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                LivenessLabel(text = livenessInstruction)
            }
        }
    }
}
