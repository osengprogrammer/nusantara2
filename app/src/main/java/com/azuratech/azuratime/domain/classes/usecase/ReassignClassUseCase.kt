package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

/**
 * UseCase to reassign a class to a different school.
 */
class ReassignClassUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(accountId: String, classId: String, newSchoolId: String): Result<Unit> =
        repository.reassignClass(accountId, classId, newSchoolId)
}
