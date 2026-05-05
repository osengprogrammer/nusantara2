package com.azuratech.azuratime.repository

import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject // 🔥 Tambahan Import

class BootRepository @Inject constructor( // 🔥 1. Tambahkan Inject Constructor
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // 🔥 Ubah menjadi suspend agar tidak memblokir Main Thread
    suspend fun getUserStatus(): String = withContext(Dispatchers.IO) {
        sessionManager.getUserStatus()
    }

    // 🔥 Pengecekan sesi sekarang berjalan di jalur IO
    suspend fun isSessionActive(): Boolean = withContext(Dispatchers.IO) {
        val isActiveStatus = sessionManager.getUserStatus() == SessionManager.STATUS_ACTIVE
        val hasActiveSchool = !sessionManager.getActiveSchoolId().isNullOrBlank()
        isActiveStatus && hasActiveSchool
    }

    fun getActiveSchoolId(): String? = sessionManager.getActiveSchoolId()
}