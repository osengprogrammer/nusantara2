package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

/**
 * UseCase to create a new school workspace.
 * Follows SSOT: Saves to Room first, sync happens in background.
 */
class CreateSchoolUseCase @Inject constructor(
    private val schoolRepository: SchoolRepository
) {
    suspend operator fun invoke(
        adminUserId: String,
        schoolName: String,
        timezone: String = "Asia/Jakarta"
    ): Result<String> {
        return schoolRepository.createSchool(adminUserId, schoolName, timezone)
    }
}
