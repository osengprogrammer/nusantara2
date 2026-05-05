package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

/**
 * UseCase to update school details (name, timezone).
 * Follows SSOT: Updates Room first, sync happens in background.
 */
class UpdateSchoolDetailsUseCase @Inject constructor(
    private val schoolRepository: SchoolRepository
) {
    suspend operator fun invoke(
        schoolId: String,
        name: String? = null,
        timezone: String? = null
    ): Result<Unit> {
        return schoolRepository.updateSchoolDetails(schoolId, name, timezone)
    }
}
