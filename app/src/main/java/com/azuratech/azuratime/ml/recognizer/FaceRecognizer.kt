package com.azuratech.azuratime.ml.recognizer

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import com.azuratech.azuratime.utils.ModelGuard
import java.util.concurrent.atomic.AtomicBoolean

object FaceRecognizer {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var tfliteModelBuffer: ByteBuffer? = null 
    
    // 🔥 Menggunakan AtomicBoolean agar aman diakses dari banyak thread kamera
    private val isInitializing = AtomicBoolean(false)
    
    var isInitialized = false
        private set

    // Buffer output tetap (Pre-allocated) untuk hemat RAM
    private val outputBuffer = Array(1) { FloatArray(FaceNetConstants.EMBEDDING_SIZE) }

    /**
     * INISIALISASI MODEL (Thread-Safe & Crash-Proof)
     */
    fun initialize(context: Context) {
        // Jika sudah siap atau sedang dalam proses, jangan double-init
        if (isInitialized || isInitializing.getAndSet(true)) return

        try {
            Log.d("AzuraBrain", "⚙️ Menyiapkan Otak AI Azura...")
            
            // 1. Baca & Decrypt via Native Guard (Super Aman!)
            val inputStream = context.assets.open(FaceNetConstants.MODEL_NAME)
            val encryptedBytes = inputStream.readBytes()
            inputStream.close()

            val guard = ModelGuard()
            val decryptedBytes = guard.decryptTfliteModel(encryptedBytes)

            // 2. Alokasi Direct Buffer (Sangat cepat untuk TFLite)
            tfliteModelBuffer = ByteBuffer.allocateDirect(decryptedBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(decryptedBytes)
                rewind() 
            }

            // Keamanan: Bersihkan ByteArray asli segera agar tidak bocor di RAM
            decryptedBytes.fill(0)

            // 3. Konfigurasi Interpreter
            val options = Interpreter.Options()
            
            // 🔥 FIX FATAL CRASH: Cek kompabilitas GPU secara aman!
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                try {
                    Log.d("AzuraBrain", "🚀 GPU Kompatibel! Mengaktifkan mode Turbo.")
                    // Gunakan opsi terbaik khusus untuk HP ini
                    gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                    options.addDelegate(gpuDelegate)
                } catch (e: Exception) {
                    Log.w("AzuraBrain", "⚠️ GPU Delegate gagal dipasang, beralih ke CPU: ${e.message}")
                    options.setNumThreads(4)
                }
            } else {
                Log.w("AzuraBrain", "⚠️ Arsitektur GPU HP ini tidak mendukung model. Menggunakan CPU (4 Threads).")
                options.setNumThreads(4) // Fallback ke CPU yang jauh lebih stabil
            }
            
            // 4. Load Model ke Interpreter
            interpreter = Interpreter(tfliteModelBuffer!!, options)
            isInitialized = true
            Log.d("AzuraBrain", "✅✅✅ JOSS! FaceRecognizer Ready & Secured")
            
        } catch (e: Exception) {
            Log.e("AzuraBrain", "❌ Initialization FAILED: ${e.message}")
            isInitialized = false
            close() // 🔥 Pastikan memori dibersihkan jika gagal agar tidak bocor
        } finally {
            isInitializing.set(false)
        }
    }

    /**
     * FUNGSI PENGENALAN (Anti-Null & Thread-Safe)
     */
    fun recognizeFace(input: ByteBuffer): FloatArray {
        // 🔥 SELF-HEALING: Jika interpreter belum siap
        val currentInterpreter = interpreter
        if (currentInterpreter == null) {
            Log.e("AzuraBrain", "⚠️ Interpreter NULL! Pastikan initialize() sukses di MainActivity/ViewModel.")
            return FloatArray(FaceNetConstants.EMBEDDING_SIZE)
        }
        
        return synchronized(this) {
            try {
                // Jalankan AI
                currentInterpreter.run(input, outputBuffer)
                
                // Ambil hasil dan lakukan Normalisasi L2 agar jarak (Distance) akurat
                val embedding = outputBuffer[0].clone()
                val normalized = l2Normalize(embedding)
                
                Log.d("AzuraBrain", "🧠 AI Berpikir... Vector: ${normalized[0]}, ${normalized[1]}")
                normalized
            } catch (e: Exception) {
                Log.e("AzuraBrain", "❌ Inference error: ${e.message}")
                FloatArray(FaceNetConstants.EMBEDDING_SIZE)
            }
        }
    }

    /**
     * Normalisasi L2: Wajib untuk Model FaceNet agar Embedding bisa dibandingan (Euclidean)
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