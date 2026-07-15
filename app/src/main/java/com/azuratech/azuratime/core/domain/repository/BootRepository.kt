package com.azuratech.azuratime.core.domain.repository

import com.azuratech.azuratime.core.result.Result
import com.google.firebase.auth.FirebaseUser

interface BootRepository {
    fun getCurrentAccount(): Result<FirebaseUser?>
    suspend fun getAccountStatus(): Result<String>
    suspend fun isSessionActive(): Result<Boolean>
    fun getActiveSchoolId(): Result<String?>
}
