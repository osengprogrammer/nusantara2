package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow

interface AccessRequestRepository {
    // UI focused methods
    suspend fun submitRequest(accountId: String, schoolId: String, schoolName: String): Result<Unit>
    suspend fun cancelRequest(accountId: String, schoolId: String): Result<Unit>
    fun observeRequestsByAccountFlow(accountId: String): Flow<Result<List<AccessRequestEntity>>>

    // Admin focused methods (matching the interface I saw earlier)
    suspend fun getPendingRequests(schoolId: String): Result<List<AccessRequestProfile>>
    suspend fun approveRequest(requestId: String): Result<Unit>
    suspend fun rejectRequest(requestId: String, reason: String): Result<Unit>
}
