package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to create a new class.
 */
class CreateClassUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(accountId: String, schoolId: String, name: String): Result<Unit> {
        // 1. Validate school existence to avoid FK failure
        val school = repository.getSchoolById(schoolId)
        if (school == null) {
            return Result.Failure(AppError.BusinessRule("Gagal: Sekolah dengan ID '$schoolId' tidak ditemukan. Pilih sekolah yang valid."))
        }

        val classModel = ClassModel(
            id = UUID.randomUUID().toString(),
            schoolId = schoolId,
            name = name,
            grade = "", // Default grade
            teacherId = null,
            studentCount = 0,
            createdAt = System.currentTimeMillis()
        )
        return repository.saveClass(accountId, schoolId, classModel)
    }
}
