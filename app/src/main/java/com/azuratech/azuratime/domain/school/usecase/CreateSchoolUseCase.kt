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
    suspend operator fun invoke(accountId: String, name: String, timezone: String): Result<String> {
        if (name.isBlank()) {
            return Result.Failure(AppError.BusinessRule("Nama sekolah tidak boleh kosong"))
        }
        
        val newId = UUID.randomUUID().toString()
        val newSchool = School(
            id = newId,
            accountId = accountId,
            name = name.trim(),
            timezone = timezone,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val result = repository.saveSchool(newSchool)
        return if (result is Result.Success) Result.Success(newId)
        else Result.Failure((result as Result.Failure).error)
    }
}
