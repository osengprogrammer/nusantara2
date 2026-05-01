package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase to get all classes for a specific school.
 */
class GetClassesUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    operator fun invoke(schoolId: String): Flow<Result<List<ClassModel>>> {
        println("🔍 GetClassesUseCase: Querying school=$schoolId")
        return repository.observeClasses(schoolId)
    }
}
