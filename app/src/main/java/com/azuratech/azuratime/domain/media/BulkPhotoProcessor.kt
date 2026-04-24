package com.azuratech.azuratime.domain.media

import com.azuratech.azuratime.domain.core.ImageProcessor
import com.azuratech.azuratime.domain.core.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Utility class for processing photos from various sources during bulk registration
 */
class BulkPhotoProcessor @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val storageProvider: StorageProvider
) {
    data class PhotoProcessResult(
        val success: Boolean,
        val imageBytes: ByteArray? = null,
        val error: String? = null
    )
    
    // --- THREADING FIX: The entire pipeline runs safely on the IO thread ---
    suspend fun processPhotoSource(
        photoSource: String,
        faceId: String
    ): PhotoProcessResult = withContext(Dispatchers.IO) {
        if (photoSource.isBlank()) {
            return@withContext PhotoProcessResult(success = true, imageBytes = null, error = null)
        }
        
        try {
            println("[$TAG] Processing photo for $faceId: ${photoSource.take(100)}...")
            
            // --- REFACTOR: Use StorageProvider to read from any source (URL, Content, File, Base64) ---
            val imageBytes = storageProvider.read(photoSource)
            
            if (imageBytes.isEmpty()) {
                return@withContext PhotoProcessResult(success = false, error = "Failed to load image from source (Check format or permissions)")
            }
            
            // --- REFACTOR: Use ImageProcessor for resizing ---
            val optimizedBytes = imageProcessor.resize(imageBytes, MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
            
            PhotoProcessResult(
                success = true,
                imageBytes = optimizedBytes
            )
            
        } catch (e: Exception) {
            println("ERROR: [$TAG] Error processing photo for $faceId: ${e.message}")
            PhotoProcessResult(success = false, error = "Processing failed: ${e.message}")
        }
    }
    
    fun getPhotoSourceType(photoSource: String): String {
        return when {
            photoSource.isBlank() -> "None"
            photoSource.startsWith("http://") || photoSource.startsWith("https://") -> "HTTP URL"
            photoSource.startsWith("content://") -> "Content Provider"
            photoSource.startsWith("data:image") -> "Base64 Data"
            photoSource.startsWith("file://") -> "File URL"
            else -> "Local Path"
        }
    }
    
    fun estimateProcessingTime(photoSources: List<String>): Long {
        var totalSeconds = 0L
        photoSources.forEach { source ->
            totalSeconds += when {
                source.isBlank() -> 0
                source.startsWith("http") -> 3 
                source.startsWith("data:") -> 1 
                else -> 1 
            }
        }
        return totalSeconds
    }

    companion object {
        private const val TAG = "BulkPhotoProcessor"
        private const val MAX_PHOTO_SIZE = 512 // pixels
    }
}
