package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update an existing check-in record.
 */
class UpdateCheckInRecordUseCase @Inject constructor(
    private val repository: CheckInRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(record: CheckInRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val recordToUpdate = record.copy(isSynced = false)
            repository.updateRecord(recordToUpdate)

            if (recordToUpdate.schoolId.isNotBlank()) {
                repository.syncRecord(recordToUpdate)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun updateClass(recordId: String, classId: String, className: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val records = repository.getCheckInRecords("", null, null, null, null, listOf(recordId), schoolId).firstOrNull()
            val record = records?.find { it.recordId == recordId }
                ?: return@withContext Result.Failure(AppError.BusinessRule("Record not found"))

            val updatedRecord = record.copy(
                classId = classId,
                className = className,
                isSynced = false
            )
            repository.updateRecord(updatedRecord)

            if (updatedRecord.schoolId.isNotBlank()) {
                repository.syncRecord(updatedRecord)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
