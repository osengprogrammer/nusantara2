package com.azuratech.azuratime.domain.media

import android.content.Context
import android.graphics.Bitmap
import java.io.File

class PhotoManager(private val context: Context) {

    /**
     * Resizes and saves a face photo to internal storage.
     * Returns the absolute path of the saved file.
     */
    fun saveFacePhoto(bitmap: Bitmap, faceId: String): String? {
        // We delegate the actual IO to your existing PhotoStorageUtils 
        // but this manager acts as the single point of entry.
        val resized = PhotoStorageUtils.resizeBitmap(bitmap, 800)
        return PhotoStorageUtils.saveFacePhoto(context, resized, faceId)
    }

    /**
     * Deletes old photos belonging to a user when they update their picture.
     */
    fun cleanupOldPhotos(faceId: String, currentPath: String) {
        PhotoStorageUtils.cleanupOldPhotos(context, faceId, currentPath)
    }

    /**
     * Safely deletes a photo file from storage.
     */
    fun deletePhoto(path: String?) {
        path?.let {
            val file = File(it)
            if (file.exists()) file.delete()
        }
    }
}