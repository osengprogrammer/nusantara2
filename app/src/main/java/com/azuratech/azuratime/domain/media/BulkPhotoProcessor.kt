package com.azuratech.azuratime.domain.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import java.util.concurrent.TimeUnit

/**
 * Utility class for processing photos from various sources during bulk registration
 */
object BulkPhotoProcessor {
    private const val TAG = "BulkPhotoProcessor"
    
    private const val MAX_PHOTO_SIZE = 512 // pixels
    private const val DOWNLOAD_TIMEOUT = 30L // seconds
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    data class PhotoProcessResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val error: String? = null
    )
    
    // --- THREADING FIX: The entire pipeline runs safely on the IO thread ---
    suspend fun processPhotoSource(
        context: Context,
        photoSource: String,
        faceId: String
    ): PhotoProcessResult = withContext(Dispatchers.IO) {
        if (photoSource.isBlank()) {
            return@withContext PhotoProcessResult(success = true, bitmap = null, error = null)
        }
        
        try {
            println("[$TAG] Processing photo for $faceId: ${photoSource.take(100)}...")
            
            val bitmap = when {
                photoSource.startsWith("http://") || photoSource.startsWith("https://") -> {
                    downloadPhotoFromUrlSafe(context, photoSource)
                }
                photoSource.startsWith("content://") -> {
                    PhotoStorageUtils.loadBitmapFromUri(context, Uri.parse(photoSource), MAX_PHOTO_SIZE)
                }
                photoSource.startsWith("data:image") -> {
                    decodeBase64Photo(photoSource)
                }
                photoSource.startsWith("file://") -> {
                    val uri = Uri.parse(photoSource)
                    PhotoStorageUtils.loadBitmapFromUri(context, uri, MAX_PHOTO_SIZE)
                }
                else -> {
                    val file = File(photoSource)
                    if (file.exists()) {
                        PhotoStorageUtils.loadBitmapFromUri(context, Uri.fromFile(file), MAX_PHOTO_SIZE)
                    } else null
                }
            }
            
            if (bitmap == null) {
                return@withContext PhotoProcessResult(success = false, error = "Failed to load image from source (Check format or permissions)")
            }
            
            // --- REDUNDANCY FIX: Use your single source of truth for resizing ---
            val optimizedBitmap = PhotoStorageUtils.resizeBitmap(bitmap, MAX_PHOTO_SIZE)
            
            // MEMORY FIX: If the resize created a smaller copy, destroy the massive original
            if (optimizedBitmap !== bitmap) {
                bitmap.recycle()
            }
            
            PhotoProcessResult(
                success = true,
                bitmap = optimizedBitmap
            )
            
        } catch (e: Exception) {
            println("ERROR: [$TAG] Error processing photo for $faceId: ${e.message}")
            PhotoProcessResult(success = false, error = "Processing failed: ${e.message}")
        }
    }
    
    private suspend fun downloadPhotoFromUrlSafe(context: Context, url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                // Use a dedicated cache folder for bulk downloads to avoid cluttering main cache
                val bulkDir = File(context.cacheDir, "bulk_temp").apply { if (!exists()) mkdirs() }
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "AzuraApp/1.0")
                    .addHeader("Accept", "image/*") // Tell server we only want images
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful || response.body == null) return@withContext null
                    
                    tempFile = File.createTempFile("dl_", ".tmp", bulkDir)
                    response.body!!.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                
                tempFile?.let {
                    // Force a smaller sample size during the first pass to save RAM
                    PhotoStorageUtils.loadBitmapFromUri(context, Uri.fromFile(it), MAX_PHOTO_SIZE)
                }
            } catch (e: Exception) {
                println("ERROR: [$TAG] Failed to download photo from $url: ${e.message}")
                null
            } finally {
                // Critical: Always delete the temp file immediately
                try { tempFile?.delete() } catch (e: Exception) {}
            }
        }
    }
    
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64Photo(dataUrl: String): Bitmap? {
        return try {
            val base64Data = if (dataUrl.contains(",")) dataUrl.substringAfter(",") else dataUrl
            val decodedBytes = Base64.decode(base64Data)
            
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
            
            // --- REDUNDANCY FIX: Reuse the shared calculateInSampleSize math ---
            options.inSampleSize = PhotoStorageUtils.calculateInSampleSize(options, MAX_PHOTO_SIZE, MAX_PHOTO_SIZE)
            options.inJustDecodeBounds = false
            
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to decode base64 photo: ${e.message}")
            null
        }
    }

    fun validatePhotoQuality(bitmap: Bitmap): List<String> {
        val issues = mutableListOf<String>()
        if (bitmap.width < 160 || bitmap.height < 160) issues.add("Photo too small (minimum 160x160 pixels)")
        if (bitmap.width > 2048 || bitmap.height > 2048) issues.add("Photo very large (will be resized)")
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (aspectRatio < 0.5f || aspectRatio > 2.0f) issues.add("Unusual aspect ratio (may affect face detection)")
        return issues
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
}