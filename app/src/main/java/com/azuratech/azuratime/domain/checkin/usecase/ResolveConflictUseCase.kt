package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.data.local.AttendanceConflict
import com.azuratech.azuratime.data.repository.UserRepository
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🏰 RESOLVE CONFLICT USE CASE
 * Resolves a data collision between Local and Cloud records.
 */
class ResolveConflictUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(conflict: AttendanceConflict, useCloud: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userRepository.resolveAttendanceConflict(conflict, useCloud)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message ?: "Gagal menyelesaikan konflik"))
        }
    }
}
