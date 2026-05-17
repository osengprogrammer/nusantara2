package com.azuratech.azuratime.features.auth.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<Pair<AccountEntity, Boolean>>
    suspend fun registerMembership(uid: String, data: Map<String, Any>): Result<Unit>
    suspend fun clearAllDataAndSignOut()
}
