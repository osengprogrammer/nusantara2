package com.azuratech.azuratime.domain.media

import javax.inject.Inject

class PhotoManager @Inject constructor(
    private val photoStorageUtils: PhotoStorageUtils
) {

    /**
     * Resizes and saves a face photo to internal storage.
     * Returns the absolute path of the saved file.
     */
    fun saveFacePhoto(imageBytes: ByteArray, faceId: String): String? {
        val resized = photoStorageUtils.resizeImage(imageBytes, 800)
        return photoStorageUtils.saveFacePhoto(resized, faceId)
    }

    /**
     * Safely deletes a photo file from storage.
     */
    fun deletePhoto(path: String?) {
        photoStorageUtils.deleteFacePhoto(path)
    }
}
