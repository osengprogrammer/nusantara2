package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.AccountProfile
import com.azuratech.azuratime.core.domain.model.AccountRole
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /**
     * 🔥 AI Native: Observe account from Room with Result wrapper.
     */
    fun getAccountFlow(id: String): Flow<Result<Account>>

    suspend fun getAccountById(id: String): Result<AccountEntity>
    fun observeAccountEntityFlow(id: String): Flow<Result<AccountEntity?>>
    suspend fun getProfile(accountId: String): Result<AccountProfile>
    suspend fun updateDisplayName(accountId: String, newName: String): Result<Unit>
    suspend fun updatePhoto(accountId: String, photoUrl: String): Result<Unit>
    suspend fun syncAccount(accountId: String): Result<AccountEntity>
    suspend fun pushAccount(accountId: String): Result<Unit>
    suspend fun searchAccounts(email: String): Result<List<AccountEntity>>

    /**
     * 🔥 ASSIGNED SCHOOL: Connect to another account within the active workspace.
     */
    suspend fun followAccount(accountId: String, targetAccountId: String): Result<Unit>
    suspend fun unfollowAccount(accountId: String, targetAccountId: String): Result<Unit>

    suspend fun updateFcmToken(accountId: String, token: String): Result<Unit>
    suspend fun sendConnectionRequest(senderId: String, targetId: String): Result<Unit>
    suspend fun acceptConnectionRequest(targetId: String, senderId: String): Result<Unit>
    suspend fun declineConnectionRequest(targetId: String, senderId: String): Result<Unit>
    fun observePendingRequestsFlow(accountId: String): Flow<Result<List<AccountEntity>>>
    fun observePendingRequestsCountFlow(accountId: String): Flow<Int>
    fun observeConnectionsFlow(accountId: String): Flow<Result<List<AccountEntity>>>

    /**
     * 🔥 ASSIGNED SCHOOL: Change role of an existing member in the Current Active School.
     */
    suspend fun assignClassToConnection(targetId: String, schoolId: String, classIds: List<String>): Result<Unit>
    suspend fun updateMemberRole(targetAccountId: String, schoolId: String, newRole: AccountRole): Result<Unit>
}
