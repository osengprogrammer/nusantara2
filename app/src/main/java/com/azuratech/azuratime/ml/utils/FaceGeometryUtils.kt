package com.azuratech.azuratime.ml.utils

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import com.azuratech.azuratime.ml.recognizer.FaceNetConstants

object FaceGeometryUtils {

    /** * Memotong wajah dari bitmap dan memastikan potongannya berbentuk KOTAK (Square).
     * Mencegah distorsi wajah saat di-resize ke 160x160.
     */
    fun cropAndPadFace(
        sourceBitmap: Bitmap, 
        faceRect: Rect, 
        padding: Float = FaceNetConstants.DEFAULT_FACE_PADDING
    ): Bitmap {
        val centerX = faceRect.centerX()
        val centerY = faceRect.centerY()

        // Base size is the longest side
        var size = max(faceRect.width(), faceRect.height())
        size += (size * padding).toInt()
        
        var halfSize = size / 2

        // 🔥 CRITICAL FIX: Ensure the square doesn't bleed off the edges
        halfSize = minOf(
            halfSize,
            centerX, 
            centerY, 
            sourceBitmap.width - centerX, 
            sourceBitmap.height - centerY
        )

        val left = centerX - halfSize
        val top = centerY - halfSize
        val finalSize = halfSize * 2

        return if (finalSize > 0) {
            Bitmap.createBitmap(sourceBitmap, left, top, finalSize, finalSize)
        } else {
            // 🔥 SAFE FALLBACK: If math fails, return a strictly bounded rectangle, NOT the whole image
            Bitmap.createBitmap(
                sourceBitmap,
                faceRect.left.coerceAtLeast(0),
                faceRect.top.coerceAtLeast(0),
                faceRect.width().coerceIn(1, sourceBitmap.width - faceRect.left.coerceAtLeast(0)),
                faceRect.height().coerceIn(1, sourceBitmap.height - faceRect.top.coerceAtLeast(0))
            )
        }
    }
}