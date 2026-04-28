package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to create or update a class.
 */
class UpdateClassUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(accountId: String, schoolId: String?, name: String, id: String? = null): Result<Unit> {
        val classModel = ClassModel(
            id = id ?: UUID.randomUUID().toString(),
            schoolId = schoolId ?: "",
            name = name,
            grade = "", // Default grade
            teacherId = null,
            studentCount = 0,
            createdAt = System.currentTimeMillis()
        )
        return repository.saveClass(accountId, schoolId, classModel)
    }
}
