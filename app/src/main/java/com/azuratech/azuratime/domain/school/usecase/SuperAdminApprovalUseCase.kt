package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.WorkspaceRepository
import javax.inject.Inject

class SuperAdminApprovalUseCase @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend fun approveSchool(schoolId: String): Result<Unit> {
        val school = schoolRepository.getSchoolById(schoolId) ?: return Result.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule("Sekolah tidak ditemukan"))
        
        val updatedSchool = school.copy(status = "ACTIVE")
        val result = schoolRepository.saveSchool(updatedSchool)
        
        if (result is Result.Success) {
            // Assign ADMIN role to the creator
            workspaceRepository.assignSchoolRole(school.accountId, schoolId, "ADMIN", school.name)
            println("✅ School '${school.name}' approved and ADMIN role assigned.")
        }
        
        return result
    }

    suspend fun rejectSchool(schoolId: String, reason: String): Result<Unit> {
        val school = schoolRepository.getSchoolById(schoolId) ?: return Result.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule("Sekolah tidak ditemukan"))
        
        // For now, we just delete or mark as ARCHIVED. Let's delete to keep it clean if rejected.
        return schoolRepository.deleteSchool(schoolId, school.accountId)
    }
}
