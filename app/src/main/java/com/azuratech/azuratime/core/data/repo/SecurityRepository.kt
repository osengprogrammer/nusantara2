package com.azuratech.azuratime.core.data.repo

import android.util.Log
import com.azuratech.azuratime.core.security.SecurityVault
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 🛡️ AZURA SECURITY REPOSITORY
 * Satu file, satu tugas: Jembatan antara Session (Data) dan JNI C++ (Hardware Check).
 * Menjamin validasi berjalan di Background Thread agar UI tidak freeze.
 */
@Singleton
class SecurityRepository @Inject constructor(private val session: SessionManager) {

    // Library JNI C++ hanya akan di-load saat fungsi ini dipanggil pertama kali
    private val vault by lazy { SecurityVault() }

    /**
     * Memeriksa integritas sistem (HMAC, Hardware ID, Time Tampering).
     * @return 1 (Valid), < 0 (Security Compromised)
     */
    suspend fun validateSecurityEnvelope(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d("AZURA_SEC", "Initiating Native Security Check...")

            if (!SecurityVault.isNativeReady) {
                Log.e("AZURA_SEC", "Native SecurityVault library is not ready!")
                return@withContext -98
            }

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

    /**
     * 🔥 SSOT: Refresh security ISO key from Cloud.
     */
    suspend fun refreshIsoKeyFromServer(): String = withContext(Dispatchers.IO) {
        try {
            val functions = FirebaseFunctions.getInstance("us-central1")

            val result = functions
                .getHttpsCallable("getSecurityIsoKey")
                .call(hashMapOf("hardwareId" to session.getHardwareId()))
                .await() 

            val response = result.data as? Map<*, *>
            val isoKey = response?.get("isoKey") as? String ?: ""
            val expireDate = (response?.get("expireDate") as? Number)?.toLong() ?: 0L

            if (isoKey.isNotBlank() && expireDate > System.currentTimeMillis()) {
                session.injectSecurityEnvelope(isoKey, expireDate)
                isoKey
            } else {
                Log.w("AZURA_SEC", "Refresh IsoKey gagal: Data tidak valid atau sudah expired.")
                ""
            }
        } catch (e: Exception) {
            Log.e("AZURA_SEC", "Refresh error: ${e.message}")
            ""
        }
    }
}
