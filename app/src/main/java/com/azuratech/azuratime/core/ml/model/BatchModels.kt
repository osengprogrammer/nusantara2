package com.azuratech.azuratime.core.ml.model

import android.graphics.Rect

/**
 * 🚀 INDIVIDUAL RECOGNITION (v3.6.0)
 * Represents a single recognized face within a multi-face batch.
 */
data class IndividualRecognition(
    val personId: String,
    val confidence: Float,
    val boundingBox: Rect,
    val faceEmbedding: FloatArray,
)

/**
 * 🚀 BATCH RECOGNITION RESULT (v3.6.0)
 * Holds results for multiple faces processed in a single frame.
 */
data class BatchRecognitionResult(
    val recognitions: List<IndividualRecognition>,
    val frameTimestamp: Long,
    val inferenceTimeMs: Long,
)
