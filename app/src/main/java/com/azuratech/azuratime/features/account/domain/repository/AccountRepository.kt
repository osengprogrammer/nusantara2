package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.AccountProfile
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun getAccountById(id: String): Result<AccountEntity>
    fun observeAccountEntity(id: String): Flow<Result<AccountEntity?>>
    suspend fun getProfile(accountId: String): Result<AccountProfile>
    suspend fun updateDisplayName(accountId: String, newName: String): Result<Unit>
    suspend fun updatePhoto(accountId: String, photoUrl: String): Result<Unit>
    suspend fun syncAccount(accountId: String): Result<AccountEntity>
    suspend fun pushAccount(accountId: String): Result<Unit>
}
