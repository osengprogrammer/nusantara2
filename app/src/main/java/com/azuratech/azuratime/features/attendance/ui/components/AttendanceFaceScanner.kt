package com.azuratech.azuratime.features.attendance.ui.components

import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.azuratech.azuratime.ml.detector.FaceAnalyzer
import com.azuratech.azuratime.core.ui.designsystem.CoreFaceCamera
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes

/**
 * AZURA FACE SCANNER
 * Main face scanner component with Liveness Detection support.
 */
@Composable
fun FaceScanner(
    useBackCamera: Boolean = false,
    shape: Shape = AzuraShapes.large,
    modifier: Modifier = Modifier,
    onLivenessStatus: (String) -> Unit, // 🔥 Instruction status: "Please Blink", etc.
    onFaceEmbedding: (Rect, FloatArray) -> Unit, // Face embedding result
) {
    // 1. Initialize Analyzer reactively to camera changes (Flip)
    val analyzer = remember(useBackCamera) {
        FaceAnalyzer(
            isFrontCamera = !useBackCamera,
            bypassLiveness = false, // 🔥 For attendance, liveness MUST be active (false)
            onFaceEmbedding = onFaceEmbedding,
            onFaceCaptured = null,
            onLivenessStatus = onLivenessStatus,
        )
    }

    // 2. Lifecycle Management: Ensure camera & detector are closed when screen changes
    DisposableEffect(analyzer) {
        onDispose {
            analyzer.close()
        }
    }

    // 3. Render Azura Core Face Camera
    CoreFaceCamera(
        analyzer = analyzer,
        useFrontCamera = !useBackCamera,
        shape = shape,
        modifier = modifier.fillMaxSize(),
    )
}
