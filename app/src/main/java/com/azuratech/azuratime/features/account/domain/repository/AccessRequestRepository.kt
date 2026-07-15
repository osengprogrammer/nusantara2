package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.core.data.local.AccessRequestEntity
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.domain.model.AccountRole
import kotlinx.coroutines.flow.Flow

interface AccessRequestRepository {
    // UI focused methods
    suspend fun submitRequest(accountId: String, schoolId: String, schoolName: String): Result<Unit>
    suspend fun cancelRequest(accountId: String, schoolId: String): Result<Unit>
    fun observeRequestsByAccountFlow(accountId: String): Flow<Result<List<AccessRequestEntity>>>

    // Admin focused methods (matching the interface I saw earlier)
    suspend fun getPendingRequests(schoolId: String): Result<List<AccessRequestProfile>>
    suspend fun approveRequest(requestId: String, role: AccountRole): Result<Unit>
    suspend fun rejectRequest(requestId: String, reason: String): Result<Unit>
}
