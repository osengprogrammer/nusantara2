package com.azuratech.azuratime.ml.recognizer

import android.graphics.Bitmap
import androidx.core.graphics.scale
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FacePreprocessor {
    
    // 🔥 FIXED: ThreadLocal gives every thread its own isolated memory pool.
    // Zero risk of buffer overwrites during concurrent camera/gallery operations.
    private val bufferThreadLocal = object : ThreadLocal<ByteBuffer>() {
        override fun initialValue(): ByteBuffer {
            return ByteBuffer.allocateDirect(FaceNetConstants.BUFFER_CAPACITY)
                .order(ByteOrder.nativeOrder())
        }
    }

    private val intValsThreadLocal = object : ThreadLocal<IntArray>() {
        override fun initialValue(): IntArray {
            return IntArray(FaceNetConstants.INPUT_SIZE * FaceNetConstants.INPUT_SIZE)
        }
    }

    fun bitmapToModelInput(faceBitmap: Bitmap): ByteBuffer {
        val scaledBitmap = faceBitmap.scale(FaceNetConstants.INPUT_SIZE, FaceNetConstants.INPUT_SIZE)

        // Fetch the dedicated buffer and array for the CURRENT executing thread
        val buffer = bufferThreadLocal.get()!!
        val intVals = intValsThreadLocal.get()!!

        buffer.rewind() 

        scaledBitmap.getPixels(
            intVals, 0, FaceNetConstants.INPUT_SIZE, 
            0, 0, FaceNetConstants.INPUT_SIZE, FaceNetConstants.INPUT_SIZE
        )

        val invStd = 1.0f / FaceNetConstants.IMAGE_STD 
        val mean = FaceNetConstants.IMAGE_MEAN

        for (i in intVals.indices) {
            val pixel = intVals[i]
            
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // Apply RGB or BGR formatting based on model configuration
            if (FaceNetConstants.USE_BGR_COLOR_FORMAT) {
                buffer.putFloat((b - mean) * invStd) 
                buffer.putFloat((g - mean) * invStd)  
                buffer.putFloat((r - mean) * invStd)        
            } else {
                buffer.putFloat((r - mean) * invStd) 
                buffer.putFloat((g - mean) * invStd)  
                buffer.putFloat((b - mean) * invStd)
            }
        }
        
        if (scaledBitmap != faceBitmap) scaledBitmap.recycle()

        buffer.rewind()
        return buffer
    }
}