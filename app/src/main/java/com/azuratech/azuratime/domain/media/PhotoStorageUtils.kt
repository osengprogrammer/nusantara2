package com.azuratech.azuratime.domain.media

import com.azuratech.azuratime.domain.core.ImageProcessor
import com.azuratech.azuratime.domain.core.StorageProvider
import javax.inject.Inject

class PhotoStorageUtils @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val storageProvider: StorageProvider
) {
    fun saveFacePhoto(imageBytes: ByteArray, faceId: String): String? {
        val fileName = "${faceId}_${System.currentTimeMillis()}.jpg"
        return try {
            storageProvider.save(imageBytes, fileName)
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to save photo: ${e.message}")
            null
        }
    }
    
    fun loadFacePhoto(filePath: String): ByteArray? {
        return try {
            storageProvider.read(filePath)
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to load photo: $filePath: ${e.message}")
            null
        }
    }
    
    /**
     * Load an image from URI/Path safely and resize it.
     */
    fun loadPhoto(uriString: String, maxDimension: Int = 1024): ByteArray? {
        return try {
            val rawBytes = storageProvider.read(uriString)
            if (rawBytes.isEmpty()) return null
            
            imageProcessor.resize(rawBytes, maxDimension, maxDimension)
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to load photo from URI: $uriString: ${e.message}")
            null
        }
    }

    fun deleteFacePhoto(filePath: String?): Boolean {
        return if (filePath != null) {
            storageProvider.delete(filePath)
        } else true
    }
    
    fun resizeImage(imageBytes: ByteArray, maxDimension: Int): ByteArray {
        return imageProcessor.resize(imageBytes, maxDimension, maxDimension)
    }

    companion object {
        private const val TAG = "PhotoStorageUtils"
    }
}
