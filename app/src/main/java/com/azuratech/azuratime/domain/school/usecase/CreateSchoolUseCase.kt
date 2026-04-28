package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.WorkspaceRepository
import java.util.*
import javax.inject.Inject

class CreateSchoolUseCase @Inject constructor(
    private val repository: SchoolRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend operator fun invoke(accountId: String, name: String, timezone: String): Result<String> {
        if (name.isBlank()) {
            return Result.Failure(AppError.BusinessRule("Nama sekolah tidak boleh kosong"))
        }
        
        // 1. Check existing school count
        val schoolCount = repository.getCountByUser(accountId)
        val status = if (schoolCount == 0) "ACTIVE" else "PENDING"
        
        val newId = UUID.randomUUID().toString()
        val newSchool = School(
            id = newId,
            accountId = accountId,
            name = name.trim(),
            timezone = timezone,
            status = status,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val result = repository.saveSchool(newSchool)
        if (result is Result.Success) {
            println("🏫 School '$name' created with status=$status")
            
            // 🔥 If ACTIVE, Auto-assign ADMIN role for the creator
            if (status == "ACTIVE") {
                workspaceRepository.assignSchoolRole(accountId, newId, "ADMIN", name.trim())
            }
            return Result.Success(newId)
        }
        
        return Result.Failure((result as Result.Failure).error)
    }
}
