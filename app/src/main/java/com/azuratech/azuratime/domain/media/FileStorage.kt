package com.azuratech.azuratime.domain.media

/**
 * Pure interface for file storage operations, removing Android dependencies from domain.
 */
interface FileStorage {
    fun saveFacePhoto(imageBytes: ByteArray, faceId: String): String?
    fun loadFacePhoto(filePath: String): ByteArray?
    fun deleteFacePhoto(filePath: String?): Boolean
}
