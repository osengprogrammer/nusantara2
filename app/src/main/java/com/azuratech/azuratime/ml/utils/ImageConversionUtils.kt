package com.azuratech.azuratime.ml.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object ImageConversionUtils {

    /**
     * Unified Singleton method to safely convert ImageProxy to Bitmap.
     * Handles both YUV (Real-time Analysis) and JPEG (Image Capture).
     *
     * @param imageProxy The frame from CameraX
     * @param isFrontCamera True if the source is the front camera
     * @param applyMirroring Set to TRUE for UI display, FALSE for ML Kit processing
     */
    fun convertImageProxyToBitmap(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean = false,
        applyMirroring: Boolean = false
    ): Bitmap? {
        
        // 1. Dapatkan original bitmap berdasarkan formatnya (Aman untuk JPEG & YUV)
        val rawBitmap: Bitmap = if (imageProxy.format == ImageFormat.JPEG) {
            val buffer = imageProxy.planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        } else {
            try {
                imageProxy.toBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        val shouldMirror = isFrontCamera && applyMirroring

        // 2. Kembalikan langsung jika tidak butuh rotasi/mirroring (Hemat RAM/CPU)
        if (rotationDegrees == 0f && !shouldMirror) {
            return rawBitmap
        }

        // 3. Fix Rotation dan opsional Mirroring
        val matrix = Matrix().apply {
            if (shouldMirror) {
                preScale(-1f, 1f)
            }
            postRotate(rotationDegrees)
        }

        val finalBitmap = Bitmap.createBitmap(
            rawBitmap, 0, 0,
            rawBitmap.width, rawBitmap.height,
            matrix, true
        )

        // 4. CRITICAL FIX: Hapus bitmap asli dari RAM agar tidak OutOfMemory
        if (finalBitmap != rawBitmap) {
            rawBitmap.recycle()
        }

        return finalBitmap
    }
}