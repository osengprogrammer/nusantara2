package com.azuratech.azuratime.domain.user.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.AccessRequestRepository
import javax.inject.Inject

/**
 * UseCase to cancel a join request or leave a school.
 * Follows SSOT: Updates Room first.
 */
class CancelSchoolAccessUseCase @Inject constructor(
    private val repository: AccessRequestRepository
) {
    suspend operator fun invoke(
        userId: String,
        schoolId: String
    ): Result<Unit> {
        return repository.cancelRequest(userId, schoolId)
    }
}
