package com.azuratech.azuratime.core.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject // 🔥 Tambahan Import

class BootRepository @Inject constructor( // 🔥 1. Tambahkan Inject Constructor
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager,
) {
    fun getCurrentAccount(): com.google.firebase.auth.FirebaseUser? = auth.currentUser

    // 🔥 Ubah menjadi suspend agar tidak memblokir Main Thread
    suspend fun getAccountStatus(): com.azuratech.azuraengine.result.Result<String> = withContext(Dispatchers.IO) {
        com.azuratech.azuraengine.result.Result.Success(sessionManager.getAccountStatus())
    }

    // 🔥 Pengecekan sesi sekarang berjalan di jalur IO
    suspend fun isSessionActive(): com.azuratech.azuraengine.result.Result<Boolean> = withContext(Dispatchers.IO) {
        com.azuratech.azuraengine.result.Result.Success(sessionManager.getAccountStatus() == SessionManager.STATUS_ACTIVE)
    }

    fun getActiveSchoolId(): String? = sessionManager.getActiveSchoolId()
}
