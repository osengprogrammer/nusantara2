package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import java.util.*
import javax.inject.Inject

class CreateSchoolUseCase @Inject constructor(
    private val repository: SchoolRepository
) {
    suspend operator fun invoke(accountId: String, name: String, timezone: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.Failure(AppError.BusinessRule("Nama sekolah tidak boleh kosong"))
        }
        
        val newSchool = School(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            name = name.trim(),
            timezone = timezone,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        return repository.saveSchool(newSchool)
    }
}
