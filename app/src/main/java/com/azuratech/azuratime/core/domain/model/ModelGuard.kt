package com.azuratech.azuratime.core.domain.model

import android.util.Log

/**
 * AZURA MODEL GUARD
 * Bertugas khusus mengamankan dan membersihkan model AI (.azr) dari RAM.
 */
class ModelGuard {

    companion object {
        var isNativeReady = false
            private set

        init {
            try {
                System.loadLibrary("azura_model_guard")
                isNativeReady = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ModelGuard", "CRITICAL: Native library azura_model_guard not found!", e)
            }
        }
    }

    /**
     * Memproses data model yang sudah didekripsi di dalam RAM.
     * Mengembalikan byte model untuk diload, dan melakukan memory scrubbing
     * pada buffer input agar tidak meninggalkan residu di memori Kotlin.
     */
    external fun secureProcessModel(decryptedBytes: ByteArray): ByteArray

    /**
     * Wrapper JNI dengan pemantauan Log RAM sebelum wajah di-scan.
     */
    fun secureProcessModelWithLogs(decryptedBytes: ByteArray): ByteArray {
        Log.d("ModelGuard_Kotlin", "Kotlin - Model size to JNI: ${decryptedBytes.size} bytes")
        if (decryptedBytes.size > 4) {
            val first4 = decryptedBytes.take(4).joinToString(" ") { "0x%02X".format(it) }
            val last4 = decryptedBytes.takeLast(4).joinToString(" ") { "0x%02X".format(it) }
            Log.d("ModelGuard_Kotlin", "Kotlin - First 4 bytes: $first4")
            Log.d("ModelGuard_Kotlin", "Kotlin - Last 4 bytes: $last4")
        }

        val result = secureProcessModel(decryptedBytes)

        Log.d("ModelGuard_Kotlin", "Kotlin - JNI execution done. Result size: ${result.size} bytes")
        return result
    }

    /**
     * Memeriksa tanda tangan FlatBuffer TFLite ('FLAT' di offset 4-7 atau offset 0-3).
     */
    fun verifyFlatBufferSignature(bytes: ByteArray): Boolean {
        if (bytes.size < 8) {
            Log.e("ModelGuard", "Error: Decrypted bytes size (${bytes.size}) is too small.")
            return false
        }

        // Cek bytes 4-7 (standar FlatBuffer file identifier)
        val sigBytes = bytes.sliceArray(4..7)
        val sig = String(sigBytes, Charsets.US_ASCII)

        // Cek juga bytes 0-3 sebagai fallback sesuai request user
        val first4Bytes = bytes.sliceArray(0..3)
        val first4Sig = String(first4Bytes, Charsets.US_ASCII)

        val isValid = sig == "FLAT" || sig == "TFL3" || first4Sig == "FLAT"

        if (!isValid) {
            val hexFirst4 = first4Bytes.joinToString(" ") { "0x%02X".format(it) }
            val hexSigBytes = sigBytes.joinToString(" ") { "0x%02X".format(it) }
            Log.e("ModelGuard", "CRITICAL: Invalid FlatBuffer signature! First 4 bytes (Hex: $hexFirst4, Text: '$first4Sig'), Bytes 4-7 (Hex: $hexSigBytes, Text: '$sig')")
        } else {
            Log.d("ModelGuard", "FlatBuffer signature verified successfully ('$sig' / '$first4Sig')")
        }

        return isValid
    }
}
