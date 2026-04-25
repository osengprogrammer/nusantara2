package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSchoolsUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    operator fun invoke(accountId: String): Flow<Result<List<School>>> =
        repository.observeSchools(accountId)
}
