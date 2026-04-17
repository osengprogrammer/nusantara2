package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.azuratech.azuratime.ml.detector.BarcodeAnalyzer
import com.azuratech.azuratime.ui.core.designsystem.CoreBarcodeCamera
import com.azuratech.azuratime.ui.theme.AzuraShapes

/**
 * AZURA BARCODE SCANNER
 * Komponen utama pemindai barcode/QR Code yang siap pakai untuk UI.
 */
@Composable
fun BarcodeScanner(
    useBackCamera: Boolean = true, // Default true karena scan barcode biasanya pakai kamera belakang
    shape: Shape = AzuraShapes.large,
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit // Hasil teks dari barcode
) {
    // 1. Inisialisasi Analyzer
    val analyzer = remember {
        BarcodeAnalyzer(
            onBarcodeDetected = onBarcodeDetected
        )
    }

    // 2. Render Kamera Core Azura khusus Barcode
    CoreBarcodeCamera(
        analyzer = analyzer,
        useFrontCamera = !useBackCamera,
        shape = shape,
        modifier = modifier.fillMaxSize()
    )
}