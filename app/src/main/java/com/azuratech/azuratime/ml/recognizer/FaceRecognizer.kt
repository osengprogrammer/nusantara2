package com.azuratech.azuratime.ml.recognizer

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import com.azuratech.azuratime.core.domain.model.ModelGuard
import java.util.concurrent.atomic.AtomicBoolean

object FaceRecognizer {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var tfliteModelBuffer: ByteBuffer? = null

    // 🔥 Using AtomicBoolean to ensure thread-safety across multiple camera threads
    private val isInitializing = AtomicBoolean(false)

    var isInitialized = false
        private set

    // Pre-allocated output buffer to optimize garbage collection and minimize RAM usage
    private val outputBuffer = Array(1) { FloatArray(FaceNetConstants.EMBEDDING_SIZE) }

    /**
     * MODEL INITIALIZATION (Thread-Safe & Crash-Proof)
     */
    fun initialize(context: Context) {
        // If already initialized or currently initializing, bypass duplicate execution
        if (isInitialized || isInitializing.getAndSet(true)) return

        try {
            Log.d("AzuraBrain", "⚙️ Preparing Azura AI Engine...")

            if (!ModelGuard.isNativeReady) {
                throw Exception("Native ModelGuard library is not ready!")
            }

            // 1. Read & Decrypt via Native Guard (Ultra Secure)
            val inputStream = context.assets.open(FaceNetConstants.MODEL_NAME)
            val encryptedBytes = inputStream.readBytes()
            inputStream.close()

            val guard = ModelGuard()
            val decryptedBytes = guard.decryptTfliteModel(encryptedBytes)

            // 2. Direct ByteBuffer Allocation (Optimized for TFLite speed)
            tfliteModelBuffer = ByteBuffer.allocateDirect(decryptedBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(decryptedBytes)
                rewind()
            }

            // Security: Instantly clear raw decrypted bytes to prevent RAM leaks
            decryptedBytes.fill(0)

            // 3. Configure Interpreter Options
            val options = Interpreter.Options()

            // 🔥 CRASH PREVENTION: Safely check GPU compatibility
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                try {
                    Log.d("AzuraBrain", "🚀 GPU Compatible! Enabling Turbo performance mode.")
                    // Apply optimized configurations tailored for this device
                    gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate)
                } catch (e: Exception) {
                    Log.w("AzuraBrain", "⚠️ Failed to attach GPU Delegate, falling back to CPU: ${e.message}")
                    options.setNumThreads(4)
                }
            } else {
                Log.w("AzuraBrain", "⚠️ Device GPU architecture does not support the model. Using CPU (4 Threads).")
                options.setNumThreads(4) // Fallback to a highly stable CPU execution
            }

            // 4. Load Model into the TFLite Interpreter
            interpreter = Interpreter(tfliteModelBuffer!!, options)
            isInitialized = true
            Log.d("AzuraBrain", "✅✅✅ SUCCESS! FaceRecognizer is Ready & Secured")
        } catch (e: Exception) {
            Log.e("AzuraBrain", "❌ Initialization FAILED: ${e.message}")
            isInitialized = false
            close() // 🔥 Ensure resources are fully cleared on failure to prevent memory leaks
        } finally {
            isInitializing.set(false)
        }
    }

    /**
     * FACE RECOGNITION (Anti-Null & Thread-Safe)
     */
    fun recognizeFace(input: ByteBuffer): FloatArray {
        // 🔥 SELF-HEALING: If interpreter is not yet initialized
        val currentInterpreter = interpreter
        if (currentInterpreter == null) {
            Log.e("AzuraBrain", "⚠️ Interpreter is NULL! Ensure initialize() runs successfully in MainActivity/ViewModel.")
            return FloatArray(FaceNetConstants.EMBEDDING_SIZE)
        }

        return synchronized(this) {
            try {
                // Execute AI inference model
                currentInterpreter.run(input, outputBuffer)

                // Fetch results and perform L2 Normalization for precise distance calculations
                val embedding = outputBuffer[0].clone()
                val normalized = l2Normalize(embedding)

                Log.d("AzuraBrain", "🧠 AI Inference Completed... Vector projection: ${normalized[0]}, ${normalized[1]}")
                normalized
            } catch (e: Exception) {
                Log.e("AzuraBrain", "❌ Inference error: ${e.message}")
                FloatArray(FaceNetConstants.EMBEDDING_SIZE)
            }
        }
    }

    /**
     * L2 Normalization: Required for FaceNet models to enable accurate Euclidean distance matching
     */
    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) sum += v * v
        val norm = sqrt(sum.coerceAtLeast(1e-10f))
        for (i in embedding.indices) {
            embedding[i] /= norm
        }
        return embedding
    }

    fun close() {
        synchronized(this) {
            interpreter?.close()
            gpuDelegate?.close()
            interpreter = null
            gpuDelegate = null
            tfliteModelBuffer?.clear()
            tfliteModelBuffer = null
            isInitialized = false
        }
    }
}
