package com.azuratech.azuratime.domain.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import android.net.Uri
import androidx.exifinterface.media.ExifInterface // Make sure this is in your build.gradle!
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object PhotoStorageUtils {
    private const val FACE_FOLDER_NAME = "faces"
    private const val TAG = "PhotoStorageUtils"
    
    fun getFacesDirectory(context: Context): File {
        val facesDir = File(context.filesDir, FACE_FOLDER_NAME)
        if (!facesDir.exists()) {
            facesDir.mkdirs()
        }
        return facesDir
    }
    
    fun saveFacePhoto(context: Context, bitmap: Bitmap, faceId: String): String? {
        return try {
            val facesDir = getFacesDirectory(context)
            val fileName = "${faceId}_${System.currentTimeMillis()}.jpg"
            val file = File(facesDir, fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            println("[$TAG] Photo saved: ${file.absolutePath}")
            file.absolutePath
        } catch (e: IOException) {
            println("ERROR: [$TAG] Failed to save photo: ${e.message}")
            null
        }
    }
    
    fun loadFacePhoto(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(filePath)
            } else {
                println("[$TAG] Photo file not found: $filePath")
                null
            }
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to load photo: $filePath: ${e.message}")
            null
        }
    }
    
    /**
     * Load a bitmap from URI (for gallery selection)
     * URIs are dangerous! They can be massive and rotated. We must load them safely.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            // 1. Decode ONLY the bounds to check the image size without loading into RAM
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // 2. Calculate the optimal downsampling ratio to prevent OOM
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false

            // 3. Decode the actual bitmap, shrunk down to a safe size
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // 4. Fix the EXIF Rotation (Gallery images are often sideways)
            rotateImageIfRequired(context, rawBitmap, uri)
            
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to load bitmap from URI: $uri: ${e.message}")
            null
        }
    }

    /**
     * Calculates the factor by which to shrink the image during loading.
     * NOW PUBLIC: Shared across the app to prevent duplicate math functions.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Reads the EXIF metadata from the URI and rotates the Bitmap so it is upright.
     */
    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val ei = input?.use { ExifInterface(it) } ?: return img

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotatedImg != img) {
            img.recycle() // Clean up the raw sideways image
        }
        return rotatedImg
    }
    
    fun deleteFacePhoto(filePath: String?): Boolean {
        return try {
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) file.delete() else true
            } else true
        } catch (e: Exception) { false }
    }
    
    fun getPhotoFileSize(filePath: String?): Long {
        return try {
            if (filePath != null) File(filePath).let { if (it.exists()) it.length() else 0L } else 0L
        } catch (e: Exception) { 0L }
    }
    
    fun cleanupOldPhotos(context: Context, faceId: String, keepFilePath: String?) {
        try {
            val facesDir = getFacesDirectory(context)
            facesDir.listFiles { file -> file.name.startsWith("${faceId}_") && file.absolutePath != keepFilePath }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            println("ERROR: [$TAG] Failed to cleanup old photos: ${e.message}")
        }
    }
    
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        
        val ratio = if (width > height) maxDimension.toFloat() / width else maxDimension.toFloat() / height
        return bitmap.scale((width * ratio).toInt(), (height * ratio).toInt())
    }
}