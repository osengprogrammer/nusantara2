package com.azuratech.azuratime.core.data.repo

import android.util.Log
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.repository.SecurityRepository
import com.azuratech.azuratime.core.security.SecurityVault
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val session: SessionManager,
) : SecurityRepository {

    private val vault by lazy { SecurityVault() }

    override suspend fun validateSecurityEnvelope(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d("AZURA_SEC", "Initiating Native Security Check...")

            if (!SecurityVault.isNativeReady) {
                Log.e("AZURA_SEC", "Native SecurityVault library is not ready!")
                return@withContext Result.Failure(AppError.BusinessRule("Native library not ready"))
            }

            val result = vault.checkAccessStatus(
                session.getLastSyncTime(),
                session.getExpireDate(),
                session.getAccountStatus(),
                session.getHardwareId(),
                session.getCloudKey(),
            )

            Log.d("AZURA_SEC", "Native Validation Result: $result")
            Result.Success(result)
        } catch (e: Exception) {
            Log.e("AZURA_SEC", "Critical JNI Error: ${e.message}")
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    override suspend fun refreshIsoKeyFromServer(): Result<String> = withContext(Dispatchers.IO) {
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
                Result.Success(isoKey)
            } else {
                Log.w("AZURA_SEC", "Refresh IsoKey gagal: Data tidak valid atau sudah expired.")
                Result.Failure(AppError.BusinessRule("Invalid or expired key"))
            }
        } catch (e: Exception) {
            Log.e("AZURA_SEC", "Refresh error: ${e.message}")
            Result.Failure(AppError.Network(e.message))
        }
    }
}
