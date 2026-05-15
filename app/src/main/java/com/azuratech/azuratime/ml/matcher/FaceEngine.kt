package com.azuratech.azuratime.ml.matcher

import com.azuratech.azuratime.ml.recognizer.FaceNetConstants

object FaceEngine {
    
    fun findBestMatch(
        inputEmbedding: FloatArray,
        gallery: List<Pair<String, FloatArray>>,
        isRegistrationMode: Boolean = false
    ): MatchResult {
        if (gallery.isEmpty()) {
            return MatchResult.NoMatch
        }

        if (!NativeSecurityVault.isNativeReady) {
            android.util.Log.e("FaceEngine", "NativeSecurityVault is not ready! Falling back to Kotlin math.")
            return findBestMatchKotlin(inputEmbedding, gallery, isRegistrationMode)
        }

        var bestMatchName = ""
        var minDistance = Float.MAX_VALUE

        // 1. Math Lock: High-speed native C++ loop
        for ((name, savedEmbedding) in gallery) {
            val distance = NativeSecurityVault.calculateDistanceNative(inputEmbedding, savedEmbedding)
            if (distance < minDistance) {
                minDistance = distance
                bestMatchName = name
            }
        }

        val currentThreshold = if (isRegistrationMode) {
            FaceNetConstants.DUPLICATE_THRESHOLD
        } else {
            FaceNetConstants.RECOGNITION_THRESHOLD
        }

        // 2. Decision Lock: Send numbers back to C++ to make the final secure choice
        val isVerified = NativeSecurityVault.verifyMatchNative(minDistance, currentThreshold)

        return when {
            isVerified && isRegistrationMode -> {
                // If similarity is found DURING REGISTRATION -> FAIL (Duplicate!)
                MatchResult.DuplicateFound(bestMatchName, minDistance)
            }
            isVerified -> {
                // If similarity is found DURING ATTENDANCE -> SUCCESS (Recognized)
                MatchResult.Success(bestMatchName, minDistance)
            }
            else -> {
                MatchResult.NoMatch
            }
        }
    }

    private fun findBestMatchKotlin(
        inputEmbedding: FloatArray,
        gallery: List<Pair<String, FloatArray>>,
        isRegistrationMode: Boolean
    ): MatchResult {
        var bestMatchName = ""
        var minDistance = Float.MAX_VALUE

        for ((name, savedEmbedding) in gallery) {
            // Cosine Distance = 1.0 - Dot Product (since vectors are L2 normalized)
            var dotProduct = 0.0f
            for (i in inputEmbedding.indices) {
                dotProduct += inputEmbedding[i] * savedEmbedding[i]
            }
            val distance = 1.0f - dotProduct
            
            if (distance < minDistance) {
                minDistance = distance
                bestMatchName = name
            }
        }

        val currentThreshold = if (isRegistrationMode) {
            FaceNetConstants.DUPLICATE_THRESHOLD
        } else {
            FaceNetConstants.RECOGNITION_THRESHOLD
        }

        val isVerified = minDistance < currentThreshold

        return when {
            isVerified && isRegistrationMode -> MatchResult.DuplicateFound(bestMatchName, minDistance)
            isVerified -> MatchResult.Success(bestMatchName, minDistance)
            else -> MatchResult.NoMatch
        }
    }

    sealed class MatchResult {
        data class Success(val name: String, val distance: Float) : MatchResult()
        data class DuplicateFound(val existingName: String, val distance: Float) : MatchResult()
        object NoMatch : MatchResult()
    }
}