package com.azuratech.azuratime.feature.auth.impl

import com.azuratech.azuratime.core.util.Result
import com.azuratech.azuratime.feature.auth.api.AuthRepository
import com.azuratech.azuratime.feature.auth.api.AuthStatus
import com.azuratech.azuratime.feature.auth.api.LoginRequest
import javax.inject.Inject

/**
 * Stub implementation – it only returns a successful “LoggedIn” status.
 * Replace the body with real Firebase Auth calls when ready.
 */
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun login(request: LoginRequest): Result<AuthStatus> {
        // TODO: Replace with real Firebase sign‑in logic.
        return Result.Success(AuthStatus.LoggedIn)
    }

    override suspend fun logout() {
        // No‑op for the stub.
    }

    override suspend fun getCurrentStatus(): Result<AuthStatus> {
        // No‑op for the stub.
        return Result.Success(AuthStatus.LoggedIn)
    }
}
