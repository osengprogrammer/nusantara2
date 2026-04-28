package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repo.SchoolRepository
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to create a new class.
 */
class CreateClassUseCase @Inject constructor(
    private val repository: SchoolRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(accountId: String, name: String, schoolId: String? = null): Result<Unit> {
        val resolvedSchoolId = schoolId ?: sessionManager.getActiveSchoolId() ?: ""
        println("🔗 DEBUG: Creating class '$name' for schoolId=$resolvedSchoolId")
        println("🔗 DEBUG: Setting direct schoolId=$resolvedSchoolId on ClassEntity")

        val classModel = ClassModel(
            id = UUID.randomUUID().toString(),
            schoolId = resolvedSchoolId, 
            name = name,
            grade = "", 
            teacherId = null,
            studentCount = 0,
            createdAt = System.currentTimeMillis()
        )
        
        val targetSchoolId = if (resolvedSchoolId.isBlank()) null else resolvedSchoolId
        return repository.saveClass(accountId, targetSchoolId, classModel)
    }
}
