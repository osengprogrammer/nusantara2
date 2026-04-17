package com.azuratech.azuratime.ml.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import com.azuratech.azuratime.ml.recognizer.FaceRecognizer
import com.azuratech.azuratime.ml.recognizer.FacePreprocessor
import com.azuratech.azuratime.ml.utils.FaceGeometryUtils
import com.azuratech.azuratime.domain.media.PhotoStorageUtils

object PhotoProcessingUtils {
    private const val TAG = "PhotoProcessingUtils"

    // --- ML KIT BOTTLENECK FIX: Lazy, reusable detector instance ---
    // This boots up exactly once and stays warm for batch processing!
    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
        )
    }

    suspend fun processBitmapForFaceEmbedding(
        @Suppress("UNUSED_PARAMETER") context: Context,
        bitmap: Bitmap
    ): Pair<Bitmap, FloatArray>? = withContext(Dispatchers.IO) {
        var processedBitmap: Bitmap? = null
        try {
            // Resize bitmap if too large for better performance
            processedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
                PhotoStorageUtils.resizeBitmap(bitmap, 1024)
            } else {
                bitmap
            }

            // Detect faces in the bitmap using ML Kit
            val faces = detectFacesInBitmap(processedBitmap)

            if (faces.isEmpty()) {
                Log.w(TAG, "No faces detected in the image")
                return@withContext null
            }

            // Use the largest face (most prominent)
            val largestFace = faces.maxByOrNull { it.width() * it.height() }
                ?: return@withContext null

            Log.d(TAG, "Processing face at: $largestFace")

            // 1) Crop using our new ML geometry foundation
            val faceBitmap = FaceGeometryUtils.cropAndPadFace(processedBitmap, largestFace)
            
            // 2) Preprocess into ByteBuffer using our cemented math
            val buffer = FacePreprocessor.bitmapToModelInput(faceBitmap)
            
            // 3) Generate the embedding
            val embedding = FaceRecognizer.recognizeFace(buffer)

            Log.d(TAG, "Successfully generated embedding with ${embedding.size} dimensions")

            Pair(faceBitmap, embedding)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process bitmap for face embedding", e)
            null
        } finally {
            // --- MEMORY FIX: Clean up the temporary resized bitmap ---
            if (processedBitmap != null && processedBitmap !== bitmap) {
                processedBitmap.recycle()
            }
        }
    }

    private suspend fun detectFacesInBitmap(bitmap: Bitmap): List<Rect> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Using the warm, reusable faceDetector!
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    val faceRects = faces.map { it.boundingBox }
                    Log.d(TAG, "Detected ${faceRects.size} faces")
                    if (continuation.isActive) {
                        continuation.resume(faceRects)
                    }
                    // NOTE: We DO NOT close the detector here anymore!
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                    // NOTE: We DO NOT close the detector here anymore!
                }
        }

    suspend fun validateFaceInBitmap(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val faces = detectFacesInBitmap(bitmap)
            val hasValidFace = faces.isNotEmpty() && faces.any { face ->
                face.width() >= 50 && face.height() >= 50
            }
            Log.d(TAG, "Face validation result: $hasValidFace (${faces.size} faces detected)")
            hasValidFace
        } catch (e: Exception) {
            Log.e(TAG, "Face validation failed", e)
            false
        }
    }

    suspend fun getFaceConfidence(bitmap: Bitmap): Float = withContext(Dispatchers.IO) {
        try {
            val faces = detectFacesInBitmap(bitmap)
            if (faces.isEmpty()) return@withContext 0.0f

            val largestFace = faces.maxByOrNull { it.width() * it.height() }
                ?: return@withContext 0.0f

            val faceArea = largestFace.width() * largestFace.height()
            val imageArea = bitmap.width * bitmap.height
            val sizeRatio = faceArea.toFloat() / imageArea.toFloat()

            (sizeRatio * 10).coerceAtMost(1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate face confidence", e)
            0.0f
        }
    }
}