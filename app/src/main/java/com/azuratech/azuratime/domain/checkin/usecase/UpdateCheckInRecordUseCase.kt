package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update an existing check-in record.
 */
class UpdateCheckInRecordUseCase @Inject constructor(
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val recordToUpdate = record.copy(isSynced = false)
            localDataSource.update(recordToUpdate)

            if (recordToUpdate.schoolId.isNotBlank()) {
                val syncRes = remoteDataSource.syncRecord(recordToUpdate)
                if (syncRes is Result.Success) {
                    localDataSource.update(recordToUpdate.copy(isSynced = true))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun updateClass(recordId: String, classId: String, className: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val record = localDataSource.getRecordById(recordId, schoolId)
                ?: return@withContext Result.Failure(AppError.BusinessRule("Record not found"))

            val updatedRecord = record.copy(
                classId = classId,
                className = className,
                isSynced = false
            )
            localDataSource.update(updatedRecord)

            if (updatedRecord.schoolId.isNotBlank()) {
                val syncRes = remoteDataSource.syncRecord(updatedRecord)
                if (syncRes is Result.Success) {
                    localDataSource.update(updatedRecord.copy(isSynced = true))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
