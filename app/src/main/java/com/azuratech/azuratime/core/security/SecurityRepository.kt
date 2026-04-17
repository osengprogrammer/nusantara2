package com.azuratech.azuratime.core.security

import android.util.Log
import com.azuratech.azuratime.security.SecurityVault
import com.azuratech.azuratime.core.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🛡️ AZURA SECURITY REPOSITORY
 * Satu file, satu tugas: Jembatan antara Session (Data) dan JNI C++ (Hardware Check).
 * Menjamin validasi berjalan di Background Thread agar UI tidak freeze.
 */
class SecurityRepository(private val session: SessionManager) {

    // Library JNI C++ hanya akan di-load saat fungsi ini dipanggil pertama kali
    private val vault by lazy { SecurityVault() }

    /**
     * Memeriksa integritas sistem (HMAC, Hardware ID, Time Tampering).
     * @return 1 (Valid), < 0 (Security Compromised)
     */
    suspend fun validateSecurityEnvelope(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d("AZURA_SEC", "Initiating Native Security Check...")
            
            val result = vault.checkAccessStatus(
                session.getLastSyncTime(),
                session.getExpireDate(),
                session.getUserStatus(),
                session.getHardwareId(),
                session.getCloudKey()
            )
            
            Log.d("AZURA_SEC", "Native Validation Result: $result")
            result
        } catch (e: Exception) {
            Log.e("AZURA_SEC", "Critical JNI Error: ${e.message}")
            -99 // Kode error internal jika JNI gagal
        }
    }
}