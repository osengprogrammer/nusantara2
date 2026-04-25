package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import javax.inject.Inject

class DeleteSchoolUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(id: String, accountId: String): Result<Unit> =
        repository.deleteSchool(id, accountId)
}
