package com.azuratech.azuratime.ml.detector

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // 1. Fokuskan hanya pada QR CODE agar scanner lebih "galak" dan cepat
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE) 
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Kita ambil hasil pertama yang terdeteksi dengan displayValue
                    val detectedValue = barcodes.firstOrNull()?.displayValue
                    if (detectedValue != null) {
                        onBarcodeDetected(detectedValue)
                    }
                }
                .addOnFailureListener { it.printStackTrace() }
                .addOnCompleteListener {
                    // Wajib ditutup agar frame kamera jalan terus
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}