package com.azuratech.azuratime.features.attendance.ui.components

import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.azuratech.azuratime.core.ui.designsystem.CoreFaceCamera
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.ml.detector.FaceAnalyzer

/**
 * AZURA FACE SCANNER
 * Komponen utama pemindai wajah dengan dukungan Liveness Detection.
 */
@Composable
fun FaceScanner(
    useBackCamera: Boolean = false, 
    shape: Shape = AzuraShapes.large,
    modifier: Modifier = Modifier,
    onLivenessStatus: (String) -> Unit, // 🔥 Status instruksi: "Silakan Berkedip", dsb.
    onFaceEmbedding: (Rect, FloatArray) -> Unit // Hasil embedding wajah
) {
    // 1. Inisialisasi Analyzer secara reaktif terhadap pergantian kamera (Flip)
    val analyzer = remember(useBackCamera) {
        FaceAnalyzer(
            isFrontCamera = !useBackCamera,
            bypassLiveness = false, // 🔥 Untuk absen, liveness HARUS aktif (false)
            onFaceEmbedding = onFaceEmbedding,
            onFaceCaptured = null,
            onLivenessStatus = onLivenessStatus
        )
    }
    
    // 2. Lifecycle Management: Pastikan kamera & detektor ditutup saat pindah layar
    DisposableEffect(analyzer) {
        onDispose { 
            analyzer.close() 
        }
    }

    // 3. Render Kamera Core Azura
    CoreFaceCamera(
        analyzer = analyzer,
        useFrontCamera = !useBackCamera,
        shape = shape,
        modifier = modifier.fillMaxSize()
    )
}