package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.AccessRequestProfile
import com.azuratech.azuratime.data.repo.AccessRequestRepository
import com.azuratech.azuratime.domain.model.AccessRequestStatus
import com.azuratech.azuratime.domain.model.SyncStatus
import javax.inject.Inject

/**
 * UseCase to submit a request to join a school.
 * Follows SSOT: Saves to Room first.
 */
class SubmitSchoolAccessUseCase @Inject constructor(
    private val repository: AccessRequestRepository
) {
    suspend operator fun invoke(
        userId: String,
        schoolId: String,
        schoolName: String,
        role: String = "TEACHER"
    ): Result<Unit> {
        val requestId = "req_${userId}_${schoolId}_${System.currentTimeMillis()}"
        val profile = AccessRequestProfile(
            requestId = requestId,
            requesterId = userId,
            schoolId = schoolId,
            schoolName = schoolName,
            status = AccessRequestStatus.PENDING,
            syncStatus = SyncStatus.PENDING_INSERT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.submitRequest(profile)
    }
}
