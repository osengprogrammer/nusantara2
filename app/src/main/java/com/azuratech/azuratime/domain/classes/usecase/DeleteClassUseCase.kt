package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

/**
 * UseCase to delete a class.
 */
class DeleteClassUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(accountId: String, schoolId: String, classId: String): Result<Unit> =
        repository.deleteClass(accountId, schoolId, classId)
}
