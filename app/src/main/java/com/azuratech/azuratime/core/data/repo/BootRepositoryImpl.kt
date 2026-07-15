package com.azuratech.azuratime.core.data.repo

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.domain.repository.BootRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionManager: SessionManager,
) : BootRepository {
    override fun getCurrentAccount(): Result<FirebaseUser?> = Result.Success(auth.currentUser)

    override suspend fun getAccountStatus(): Result<String> = withContext(Dispatchers.IO) {
        Result.Success(sessionManager.getAccountStatus())
    }

    override suspend fun isSessionActive(): Result<Boolean> = withContext(Dispatchers.IO) {
        Result.Success(sessionManager.getAccountStatus() == SessionManager.STATUS_ACTIVE)
    }

    override fun getActiveSchoolId(): Result<String?> = Result.Success(sessionManager.getActiveSchoolId())
}
