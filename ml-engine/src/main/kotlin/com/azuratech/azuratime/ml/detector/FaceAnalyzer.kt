package com.azuratech.azuratime.ml.detector

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.azuratech.azuratime.ml.utils.FaceGeometryUtils
import com.azuratech.azuratime.ml.utils.ImageConversionUtils
import com.azuratech.azuratime.ml.recognizer.FaceRecognizer
import com.azuratech.azuratime.ml.recognizer.FacePreprocessor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class FaceAnalyzer(
    private val isFrontCamera: Boolean = true,
    private val bypassLiveness: Boolean = false,
    private val onFaceEmbedding: (Rect, FloatArray) -> Unit,
    private val onFaceCaptured: ((Bitmap) -> Unit)? = null,
    private val onLivenessStatus: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val analyzerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build(),
    )

    private var isEyeClosed = false
    private var hasBlinked = false
    private val BLINK_THRESHOLD = 0.25f

    var faceBounds by mutableStateOf<List<Rect>>(emptyList())
        private set

    var imageSize by mutableStateOf(IntSize(0, 0))
        private set

    var imageRotation by mutableStateOf(0)
        private set

    private val isProcessing = AtomicBoolean(false)
    private var lastProcessTime = 0L
    private val throttleInterval = 100L // Beri nafas untuk CPU
    private var lastLogTime = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // --- 🔥 SAFETY GUARD: Pastikan AI Brain sudah siap sebelum memproses frame ---
        if (!FaceRecognizer.isReady.value) {
            // Log secara berkala agar tidak membanjiri Logcat (sekali per 2 detik)
            val now = System.currentTimeMillis()
            if (now - lastLogTime > 2000L) {
                Log.w("Azura_Analyzer", "⚠️ Frame DROPPED: FaceRecognizer is NOT ready yet! Check your .azr asset or model initialization logs.")
                lastLogTime = now
            }
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()

        // 1. Throttle & Lock Check
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }
        if (currentTime - lastProcessTime < throttleInterval) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Log.e("Azura_Analyzer", "❌ Frame DROPPED: mediaImage is NULL")
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        lastProcessTime = currentTime

        val rotation = imageProxy.imageInfo.rotationDegrees
        imageRotation = rotation

        val isPortrait = rotation == 90 || rotation == 270
        imageSize = if (isPortrait) {
            IntSize(mediaImage.height, mediaImage.width)
        } else {
            IntSize(mediaImage.width, mediaImage.height)
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                faceBounds = faces.map { it.boundingBox }

                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    Log.d("Azura_Analyzer", "👤 Face DETECTED! BoundingBox: ${face.boundingBox}. Checking liveness...")

                    // --- LIVENESS LOGIC ---
                    if (!bypassLiveness && !hasBlinked) {
                        val leftEye = face.leftEyeOpenProbability ?: 1.0f
                        val rightEye = face.rightEyeOpenProbability ?: 1.0f
                        Log.d("Azura_Analyzer", "👁️ Eye Probability - Left: $leftEye, Right: $rightEye (Blink Threshold: $BLINK_THRESHOLD)")

                        if (leftEye < BLINK_THRESHOLD && rightEye < BLINK_THRESHOLD) {
                            isEyeClosed = true
                            Log.i("Azura_Analyzer", "👁️ Eyes closed detected. Waiting for eyes to reopen to complete blink.")
                            onLivenessStatus("Mata Tertutup...")
                        } else if (isEyeClosed && leftEye > 0.6f && rightEye > 0.6f) {
                            hasBlinked = true
                            isEyeClosed = false
                            Log.i("Azura_Analyzer", "✅ Liveness passed: Blink detected!")
                            onLivenessStatus("Kedipan Terdeteksi!")
                        } else {
                            onLivenessStatus("Silakan Berkedip")
                        }

                        if (!hasBlinked) {
                            imageProxy.close()
                            isProcessing.set(false)
                            return@addOnSuccessListener
                        }
                    } else if (bypassLiveness) {
                        Log.d("Azura_Analyzer", "⚡ Liveness check bypassed by configuration.")
                    }

                    // --- 🔥 PERBAIKAN KRUSIAL: EKSTRAK BITMAP DI SINI (SINKRON) ---
                    val safeBitmap = try {
                        ImageConversionUtils.convertImageProxyToBitmap(
                            imageProxy = imageProxy,
                            isFrontCamera = isFrontCamera,
                            applyMirroring = false,
                        )
                    } catch (e: Exception) {
                        Log.e("FaceAnalyzer", "Gagal convert ImageProxy: ${e.message}")
                        null
                    }

                    // 🛑 SEKARANG AMAN UNTUK MENUTUP PROXY
                    imageProxy.close()

                    // --- PROCESS EMBEDDING DI BACKGROUND ---
                    if (safeBitmap != null) {
                        val bounds = face.boundingBox

                        analyzerScope.launch {
                            try {
                                Log.d("Azura_Analyzer", "Wajah terdeteksi: $bounds. Memulai inferensi FaceRecognizer...")
                                val safeCrop = FaceGeometryUtils.cropAndPadFace(safeBitmap, bounds)
                                val buffer = FacePreprocessor.bitmapToModelInput(safeCrop)
                                val embedding = FaceRecognizer.recognizeFace(buffer)
                                Log.d("Azura_Analyzer", "Inferensi FaceRecognizer selesai. Ukuran embedding: ${embedding.size}")

                                withContext(Dispatchers.Main) {
                                    onFaceEmbedding(bounds, embedding)
                                    onFaceCaptured?.invoke(safeCrop)
                                }

                                if (safeCrop != safeBitmap) safeCrop.recycle()
                                safeBitmap.recycle()
                            } catch (e: Exception) {
                                Log.e("FaceAnalyzer", "Error di Coroutine: ${e.message}")
                            } finally {
                                isProcessing.set(false)
                            }
                        }
                    } else {
                        isProcessing.set(false)
                    }
                } else {
                    // Reset liveness jika wajah hilang dari frame kamera
                    hasBlinked = false
                    isEyeClosed = false
                    onLivenessStatus("Cari Wajah...")
                    imageProxy.close()
                    isProcessing.set(false) // 🔓 Release lock immediately
                }
            }
            .addOnFailureListener {
                Log.e("FaceAnalyzer", "ML Kit Error: ${it.message}")
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun close() {
        detector.close()
    }
}
