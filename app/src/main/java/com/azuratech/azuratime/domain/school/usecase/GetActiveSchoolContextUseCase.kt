package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SchoolContext(
    val schoolId: String,
    val role: String
)

/**
 * UseCase to resolve the active school context (schoolId and role).
 * Centralizes validation to prevent scattered null-checks and empty-string fallbacks.
 */
class GetActiveSchoolContextUseCase @Inject constructor(
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) {
    suspend operator fun invoke(): Result<SchoolContext> = withContext(Dispatchers.IO) {
        val activeSchoolId = sessionManager.getActiveSchoolId()
        if (activeSchoolId.isNullOrBlank()) {
            return@withContext Result.Failure(AppError.BusinessRule("No school selected"))
        }

        val userId = sessionManager.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            return@withContext Result.Failure(AppError.BusinessRule("No user session found"))
        }

        val user = database.userDao().getUserById(userId)
            ?: return@withContext Result.Failure(AppError.BusinessRule("User not found in local database"))

        // 1. Check for Global SUPER_ADMIN access
        if (user.role == "SUPER_ADMIN") {
            return@withContext Result.Success(SchoolContext(activeSchoolId, "SUPER_ADMIN"))
        }

        // 2. Check for Multi-tenant Membership
        val membership = user.memberships[activeSchoolId]
        if (membership != null) {
            return@withContext Result.Success(SchoolContext(activeSchoolId, membership.role))
        }

        // 3. Fallback: Not authorized
        Result.Failure(AppError.BusinessRule("Not authorized for this school"))
    }
}
