package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

class UnassignClassFromSchoolUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(schoolId: String, classId: String): Result<Unit> {
        return repository.unassignClassFromSchool(schoolId, classId)
    }
}
