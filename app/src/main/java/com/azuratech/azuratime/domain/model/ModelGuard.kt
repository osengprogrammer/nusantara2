package com.azuratech.azuratime.utils

/**
 * AZURA MODEL GUARD
 * Bertugas khusus mengamankan dan mendekripsi model AI (.azr) dari RAM.
 */
class ModelGuard {

    companion object {
        init {
            // Load library C++ baru yang kita daftarkan di CMakeLists.txt
            System.loadLibrary("azura_model_guard")
        }
    }

    /**
     * Membuka gembok file .azr langsung di dalam RAM.
     */
    external fun decryptTfliteModel(encryptedBytes: ByteArray): ByteArray
}