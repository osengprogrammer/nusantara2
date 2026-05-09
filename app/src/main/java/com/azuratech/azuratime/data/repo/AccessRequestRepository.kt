package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.model.AccessRequestProfile
import kotlinx.coroutines.flow.Flow

import com.azuratech.azuratime.data.local.AccessRequestEntity

interface AccessRequestRepository {
    suspend fun submitRequest(userId: String, schoolId: String, schoolName: String): Result<Unit>
    suspend fun cancelRequest(requesterId: String, schoolId: String): Result<Unit>
    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>>
}
