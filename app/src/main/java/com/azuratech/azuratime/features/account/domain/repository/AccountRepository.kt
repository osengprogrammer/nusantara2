package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.AccountProfile
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /**
     * 🔥 AI Native: Observe account from Room with Result wrapper.
     */
    fun getAccount(id: String): Flow<Result<Account>>

    suspend fun getAccountById(id: String): Result<AccountEntity>
    fun observeAccountEntity(id: String): Flow<Result<AccountEntity?>>
    suspend fun getProfile(accountId: String): Result<AccountProfile>
    suspend fun updateDisplayName(accountId: String, newName: String): Result<Unit>
    suspend fun updatePhoto(accountId: String, photoUrl: String): Result<Unit>
    suspend fun syncAccount(accountId: String): Result<AccountEntity>
    suspend fun pushAccount(accountId: String): Result<Unit>
    suspend fun searchAccounts(email: String): Result<List<AccountEntity>>
    suspend fun followAccount(accountId: String, targetAccountId: String): Result<Unit>
    suspend fun unfollowAccount(accountId: String, targetAccountId: String): Result<Unit>
    suspend fun updateFcmToken(accountId: String, token: String): Result<Unit>
    suspend fun sendConnectionRequest(senderId: String, targetId: String): Result<Unit>
    suspend fun acceptConnectionRequest(targetId: String, senderId: String): Result<Unit>
    suspend fun declineConnectionRequest(targetId: String, senderId: String): Result<Unit>
    fun observePendingRequests(accountId: String): Flow<Result<List<AccountEntity>>>
    fun observePendingRequestsCount(accountId: String): Flow<Int>
    fun observeConnections(accountId: String): Flow<Result<List<AccountEntity>>>
    suspend fun assignClassToConnection(targetId: String, schoolId: String, classIds: List<String>): Result<Unit>
}
