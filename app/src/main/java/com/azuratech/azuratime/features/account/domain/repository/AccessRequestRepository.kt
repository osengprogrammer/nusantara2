package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import kotlinx.coroutines.flow.Flow

interface AccessRequestRepository {
    // UI focused methods
    suspend fun submitRequest(userId: String, schoolId: String, schoolName: String): Result<Unit>
    suspend fun cancelRequest(requesterId: String, schoolId: String): Result<Unit>
    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>>

    // Admin focused methods (matching the interface I saw earlier)
    suspend fun getPendingRequests(schoolId: String): List<AccessRequestProfile>
    suspend fun approveRequest(requestId: String): Boolean
    suspend fun rejectRequest(requestId: String, reason: String): Boolean
}
