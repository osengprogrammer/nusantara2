package com.azuratech.azuratime.core.domain.model

/**
 * AZURA MODEL GUARD
 * Bertugas khusus mengamankan dan mendekripsi model AI (.azr) dari RAM.
 */
class ModelGuard {

    companion object {
        var isNativeReady = false
            private set

        init {
            try {
                // Load library C++ baru yang kita daftarkan di CMakeLists.txt
                System.loadLibrary("azura_model_guard")
                isNativeReady = true
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("ModelGuard", "CRITICAL: Native library azura_model_guard not found!", e)
            }
        }
    }

    /**
     * Membuka gembok file .azr langsung di dalam RAM.
     */
    external fun decryptTfliteModel(encryptedBytes: ByteArray): ByteArray
}
