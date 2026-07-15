package com.azuratech.azuratime.features.auth.domain.repository

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.account.domain.model.Account

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<Pair<Account, Boolean>>
    suspend fun registerMembership(uid: String, data: Map<String, Any>): Result<Unit>
    suspend fun clearAllDataAndSignOut(): Result<Unit>
}
