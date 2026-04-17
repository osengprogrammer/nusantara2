package com.azuratech.azuratime.ml.recognizer

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import com.azuratech.azuratime.utils.ModelUtils
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * Azura Face Recognition Engine - Optimized TFLite Version.
 * Features Pre-allocated Memory to prevent Garbage Collection stutters!
 */
object FaceRecognizer {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    
    var isInitialized = false
        private set

    // 🔥 THE FIX: Pre-allocated output array. Created once, reused infinitely!
    private val outputBuffer = Array(1) { FloatArray(FaceNetConstants.EMBEDDING_SIZE) }

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val modelBuffer = ModelUtils.loadModelFile(context, FaceNetConstants.MODEL_NAME)
            val options = Interpreter.Options()

            // Try to enable GPU acceleration
            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
                Log.d("Azura", "✅ GPU Acceleration Enabled")
            } catch (e: Exception) {
                Log.w("Azura", "⚠️ GPU not supported, falling back to CPU")
                gpuDelegate?.close() // Ensure resources are freed if addDelegate fails
                gpuDelegate = null
                options.setNumThreads(4)
            }
            
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Log.d("Azura", "✅ FaceRecognizer Ready")
        } catch (e: Exception) {
            Log.e("Azura", "❌ Initialization failed: ${e.message}")
            close() // Reset everything on total failure
        }
    }

    fun recognizeFace(input: ByteBuffer): FloatArray {
        // Use thread-safe access
        val currentInterpreter = interpreter ?: return FloatArray(FaceNetConstants.EMBEDDING_SIZE)
        
        return try {
            // Internal synchronization so multiple threads (like Gallery Upload + Camera) don't crash it
            synchronized(this) {
                currentInterpreter.run(input, outputBuffer)
                // 🔥 THE FIX: Define 'sample' before using it in Log
            val sample = outputBuffer[0]
                Log.d("AzuraBrain", "Sample: ${sample[0]}, ${sample[1]}, ${sample[2]}")
                // 🔥 We MUST .clone() the result so we don't accidentally overwrite it on the next frame!
                l2Normalize(outputBuffer[0].clone())
            }
        } catch (e: Exception) {
            Log.e("Azura", "❌ Inference error: ${e.message}")
            FloatArray(FaceNetConstants.EMBEDDING_SIZE)
        }
    }

    /**
     * L2 Normalization limits the vector distance to a unit circle (0.0 - 1.0).
     */
    // private fun l2Normalize(embedding: FloatArray): FloatArray {
    //     var sum = 0f
    //     for (v in embedding) sum += v * v
        
    //     // Prevent NaN if sum approaches zero
    //     val norm = sqrt(sum.coerceAtLeast(1e-10f))
    //     for (i in embedding.indices) {
    //         embedding[i] /= norm
    //     }
    //     return embedding
    // }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
    var sum = 0f
    for (v in embedding) sum += v * v
    val norm = sqrt(sum.coerceAtLeast(1e-10f)) // Ensure this epsilon is small
    for (i in embedding.indices) {
        embedding[i] /= norm
    }
    return embedding
}

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isInitialized = false
    }
}