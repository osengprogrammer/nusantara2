package com.azuratech.azuratime.data.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.azuratech.azuraengine.core.ImageProcessor
import com.azuratech.azuratime.ml.utils.PhotoProcessingUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageProcessor {

    override fun resize(imageBytes: ByteArray, maxWidth: Int, maxHeight: Int): ByteArray {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options) ?: return imageBytes
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return if (options.inSampleSize > 1) {
                val res = bitmapToByteArray(bitmap)
                bitmap.recycle()
                res
            } else {
                bitmap.recycle()
                imageBytes
            }
        }

        val ratio = Math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            (width * ratio).toInt(),
            (height * ratio).toInt(),
            true
        )
        
        val result = bitmapToByteArray(scaledBitmap)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        return result
    }

    override fun rotate(imageBytes: ByteArray, degrees: Int): ByteArray {
        if (degrees % 360 == 0) return imageBytes
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        
        val result = bitmapToByteArray(rotatedBitmap)
        if (rotatedBitmap != bitmap) {
            rotatedBitmap.recycle()
        }
        bitmap.recycle()
        return result
    }

    override suspend fun extractFaceEmbedding(imageBytes: ByteArray): Pair<ByteArray, FloatArray>? {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        
        val result = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap)
        bitmap.recycle()
        
        return result?.let { (faceBitmap, embedding) ->
            val faceBytes = bitmapToByteArray(faceBitmap)
            faceBitmap.recycle()
            faceBytes to embedding
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
