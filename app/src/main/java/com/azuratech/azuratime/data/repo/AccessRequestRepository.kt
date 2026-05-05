package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.AccessRequestProfile
import kotlinx.coroutines.flow.Flow

interface AccessRequestRepository {
    suspend fun submitRequest(profile: AccessRequestProfile): Result<Unit>
    suspend fun cancelRequest(requesterId: String, schoolId: String): Result<Unit>
    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestProfile>>
}
