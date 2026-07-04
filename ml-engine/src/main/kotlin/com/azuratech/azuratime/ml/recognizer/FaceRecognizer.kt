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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FaceRecognizer {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var tfliteModelBuffer: ByteBuffer? = null

    // 🔥 Menggunakan AtomicBoolean agar aman diakses dari banyak thread kamera
    private val isInitializing = AtomicBoolean(false)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private fun setReady(value: Boolean) {
        if (_isReady.value != value) {
            _isReady.value = value
            Log.d("Azura_State", "AI Ready status changed: $value")
        }
    }

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

            if (!ModelGuard.isNativeReady) {
                throw Exception("Native ModelGuard library is not ready!")
            }

            // 1. Read & Decrypt via Native Guard (Super Aman!)
            val inputStream = context.assets.open(FaceNetConstants.MODEL_NAME)
            val encryptedBytes = inputStream.readBytes()
            inputStream.close()

            Log.d("Azura_Init", "ModelGuard JNI process started")
            val decryptedBytesFromCrypto = com.azuratech.azuratime.core.security.CryptoEngine.decryptModel(encryptedBytes)
            val guard = ModelGuard()
            val decryptedBytes = guard.secureProcessModelWithLogs(decryptedBytesFromCrypto)
            Log.d("Azura_Init", "ModelGuard JNI process finished")

            if (decryptedBytes.isEmpty()) {
                throw Exception("Decrypted model bytes are empty!")
            }
            if (!guard.verifyFlatBufferSignature(decryptedBytes)) {
                throw Exception("Decrypted model bytes do not have a valid FlatBuffer signature!")
            }
            Log.d("AzuraBrain", "🔓 Model decrypted: ${decryptedBytes.size} bytes")

            // 2. Direct ByteBuffer Allocation (Avoid file/file-channel alignment issues)
            val modelBuffer = ByteBuffer.allocateDirect(decryptedBytes.size)
                .order(ByteOrder.nativeOrder())
            modelBuffer.put(decryptedBytes)
            modelBuffer.rewind() // Reset position to 0 so TFLite reads from start
            tfliteModelBuffer = modelBuffer

            Log.d("AzuraBrain", "✅ Model loaded successfully via DirectByteBuffer (Aligned & RAM-safe).")

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
            setReady(true)
            Log.d("AzuraBrain", "✅✅✅ JOSS! FaceRecognizer Ready & Secured")
        } catch (e: Exception) {
            Log.e("AzuraBrain", "❌ Initialization FAILED: ${e.message}")
            isInitialized = false
            setReady(false)
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
                // 🔍 DIAGNOSTIC LOG FOR INPUT BUFFER
                val dup = input.duplicate().order(input.order())
                dup.rewind()
                val samples = FloatArray(5)
                for (i in 0..4) {
                    if (dup.remaining() >= 4) {
                        samples[i] = dup.getFloat()
                    }
                }
                Log.d("Azura_Buffer", "Input buffer capacity: ${input.capacity()}, position: ${input.position()}, limit: ${input.limit()}, samples: [${samples.joinToString(", ")}]")

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
            setReady(false)
        }
    }
}
