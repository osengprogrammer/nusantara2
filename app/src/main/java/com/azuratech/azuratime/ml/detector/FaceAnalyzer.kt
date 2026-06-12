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
import com.azuratech.azuratime.core.ml.model.IndividualRecognition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class FaceAnalyzer(
    private val isFrontCamera: Boolean = true,
    private val bypassLiveness: Boolean = false,
    private val onFaceEmbedding: (Rect, FloatArray) -> Unit,
    private val onBatchFaceEmbedding: ((List<IndividualRecognition>) -> Unit)? = null,
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
                    // --- LIVENESS LOGIC (Single face only for now) ---
                    if (!bypassLiveness && !hasBlinked && faces.size == 1) {
                        val face = faces[0]
                        val leftEye = face.leftEyeOpenProbability ?: 1.0f
                        val rightEye = face.rightEyeOpenProbability ?: 1.0f

                        if (leftEye < BLINK_THRESHOLD && rightEye < BLINK_THRESHOLD) {
                            isEyeClosed = true
                            onLivenessStatus("Eyes Closed...")
                        } else if (isEyeClosed && leftEye > 0.6f && rightEye > 0.6f) {
                            hasBlinked = true
                            isEyeClosed = false
                            onLivenessStatus("Blink Detected!")
                        } else {
                            onLivenessStatus("Please Blink")
                        }

                        if (!hasBlinked) {
                            imageProxy.close()
                            isProcessing.set(false)
                            return@addOnSuccessListener
                        }
                    }

                    // --- EKSTRAK BITMAP ---
                    val safeBitmap = try {
                        ImageConversionUtils.convertImageProxyToBitmap(
                            imageProxy = imageProxy,
                            isFrontCamera = isFrontCamera,
                            applyMirroring = false,
                        )
                    } catch (e: Exception) {
                        Log.e("FaceAnalyzer", "Failed to convert ImageProxy: ${e.message}")
                        null
                    }

                    imageProxy.close()

                    // --- PROCESS DI BACKGROUND ---
                    if (safeBitmap != null) {
                        analyzerScope.launch {
                            try {
                                if (onBatchFaceEmbedding != null && faces.size > 1) {
                                    // 🔥 v3.6.0: Parallel Batch Processing
                                    val recognitions = faces.map { face ->
                                        async {
                                            val crop = FaceGeometryUtils.cropAndPadFace(safeBitmap, face.boundingBox)
                                            val buffer = FacePreprocessor.bitmapToModelInput(crop)
                                            val emb = FaceRecognizer.recognizeFace(buffer)
                                            IndividualRecognition(
                                                personId = "PENDING",
                                                confidence = 0f,
                                                boundingBox = face.boundingBox,
                                                faceEmbedding = emb,
                                            )
                                        }
                                    }.awaitAll()

                                    withContext(Dispatchers.Main) {
                                        onBatchFaceEmbedding.invoke(recognitions)
                                    }
                                } else {
                                    // Standard Single Processing
                                    val bounds = faces[0].boundingBox
                                    val safeCrop = FaceGeometryUtils.cropAndPadFace(safeBitmap, bounds)
                                    val buffer = FacePreprocessor.bitmapToModelInput(safeCrop)
                                    val embedding = FaceRecognizer.recognizeFace(buffer)

                                    withContext(Dispatchers.Main) {
                                        onFaceEmbedding(bounds, embedding)
                                        onFaceCaptured?.invoke(safeCrop)
                                    }
                                }
                                safeBitmap.recycle()
                            } catch (e: Exception) {
                                Log.e("FaceAnalyzer", "Error in Coroutine: ${e.message}")
                            } finally {
                                isProcessing.set(false)
                            }
                        }
                    } else {
                        isProcessing.set(false)
                    }
                } else {
                    hasBlinked = false
                    isEyeClosed = false
                    onLivenessStatus("Searching for Face...")
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
