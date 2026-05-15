package com.azuratech.azuratime.core.domain.media

interface ImageProcessor {
    suspend fun process(imageBytes: ByteArray): ByteArray
}
