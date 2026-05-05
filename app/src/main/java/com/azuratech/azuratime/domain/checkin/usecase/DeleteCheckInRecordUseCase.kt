package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to delete a check-in record.
 */
class DeleteCheckInRecordUseCase @Inject constructor(
    private val repository: CheckInRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(recordId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school found"))

            repository.deleteRecord(recordId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
