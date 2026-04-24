package com.azuratech.azuratime.domain.core

interface ImageProcessor {
    fun resize(imageBytes: ByteArray, maxWidth: Int, maxHeight: Int): ByteArray
    fun rotate(imageBytes: ByteArray, degrees: Int): ByteArray
    suspend fun extractFaceEmbedding(imageBytes: ByteArray): Pair<ByteArray, FloatArray>?
}
