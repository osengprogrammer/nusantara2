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
    private val onLivenessStatus: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val analyzerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) 
            .enableTracking()
            .build()
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

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // 1. Throttle & Lock Check
        if (isProcessing.get() || (currentTime - lastProcessTime < throttleInterval)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
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

                    // --- LIVENESS LOGIC ---
                    if (!bypassLiveness && !hasBlinked) {
                        val leftEye = face.leftEyeOpenProbability ?: 1.0f
                        val rightEye = face.rightEyeOpenProbability ?: 1.0f

                        if (leftEye < BLINK_THRESHOLD && rightEye < BLINK_THRESHOLD) {
                            isEyeClosed = true
                            onLivenessStatus("Mata Tertutup...")
                        } else if (isEyeClosed && leftEye > 0.6f && rightEye > 0.6f) {
                            hasBlinked = true
                            isEyeClosed = false
                            onLivenessStatus("Kedipan Terdeteksi!")
                        } else {
                            onLivenessStatus("Silakan Berkedip")
                        }
                        
                        if (!hasBlinked) {
                            imageProxy.close()
                            isProcessing.set(false)
                            return@addOnSuccessListener
                        }
                    }

                    // --- 🔥 PERBAIKAN KRUSIAL: EKSTRAK BITMAP DI SINI (SINKRON) ---
                    // Jangan mengekstrak imageProxy di dalam coroutine (background) 
                    // karena sistem bisa menutupnya kapan saja.
                    val safeBitmap = try {
                        ImageConversionUtils.convertImageProxyToBitmap(
                            imageProxy = imageProxy,
                            isFrontCamera = isFrontCamera,
                            applyMirroring = false
                        )
                    } catch (e: Exception) {
                        Log.e("FaceAnalyzer", "Gagal convert ImageProxy: ${e.message}")
                        null
                    }

                    // 🛑 SEKARANG AMAN UNTUK MENUTUP PROXY (RAM langsung lega)
                    imageProxy.close()

                    // --- PROCESS EMBEDDING DI BACKGROUND ---
                    if (safeBitmap != null) {
                        val bounds = face.boundingBox
                        
                        analyzerScope.launch {
                            try {
                                // Potong wajah, convert ke TFLite buffer, dan jalankan AI
                                val safeCrop = FaceGeometryUtils.cropAndPadFace(safeBitmap, bounds)
                                val buffer = FacePreprocessor.bitmapToModelInput(safeCrop)
                                val embedding = FaceRecognizer.recognizeFace(buffer)

                                // Bersihkan memori Bitmap secara manual agar tidak memory leak
                                if (safeCrop != safeBitmap) safeCrop.recycle()
                                safeBitmap.recycle() 

                                // Kirim hasil ke Main Thread (UI/ViewModel)
                                withContext(Dispatchers.Main) {
                                    onFaceEmbedding(bounds, embedding)
                                }
                            } catch (e: Exception) {
                                Log.e("FaceAnalyzer", "Error di Coroutine: ${e.message}")
                            } finally {
                                // 🔓 Buka gembok processing agar frame berikutnya bisa masuk
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
                    isProcessing.set(false)
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