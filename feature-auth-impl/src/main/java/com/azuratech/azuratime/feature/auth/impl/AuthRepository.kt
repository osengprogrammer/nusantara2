package com.azuratech.azuratime.feature.auth.api

import com.azuratech.azuratime.core.util.Result

interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<AuthStatus>
    suspend fun logout()
    suspend fun getCurrentStatus(): Result<AuthStatus>
}
