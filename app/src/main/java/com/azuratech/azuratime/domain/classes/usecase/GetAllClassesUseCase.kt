package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to get all classes for an account across all schools.
 */
class GetAllClassesUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    operator fun invoke(accountId: String): Flow<Result<List<ClassModel>>> =
        repository.observeAllClassesForAccount(accountId)
}
