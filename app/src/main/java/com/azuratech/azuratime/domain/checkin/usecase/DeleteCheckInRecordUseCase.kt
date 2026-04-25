package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
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
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(recordId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val record = localDataSource.getRecordById(recordId, schoolId)
                ?: return@withContext Result.Failure(AppError.BusinessRule("Record not found"))

            localDataSource.delete(record)

            if (record.schoolId.isNotBlank()) {
                val deleteRes = remoteDataSource.deleteRecord(schoolId, record.id)
                if (deleteRes is Result.Failure) {
                    // Log or handle remote deletion failure if needed
                    // For now, following current repo behavior
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
