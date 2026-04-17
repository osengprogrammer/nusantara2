package com.azuratech.azuratime.ml.matcher

import com.azuratech.azuratime.ml.recognizer.FaceNetConstants

object FaceDistanceUtils {

    /**
     * Optimized Cosine Distance for Azura Tech Engine.
     * ZERO square roots. Lightning fast because inputs are already L2-normalized.
     * Result is between 0.0 (identical) and 2.0 (completely opposite).
     */
    fun calculateDistance(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        return 1f - dotProduct.coerceIn(-1f, 1f)
    }

    /**
     * Evaluates if the distance meets the criteria for a match.
     */
    fun isMatch(distance: Float): Boolean {
        return distance < FaceNetConstants.RECOGNITION_THRESHOLD
    }
}