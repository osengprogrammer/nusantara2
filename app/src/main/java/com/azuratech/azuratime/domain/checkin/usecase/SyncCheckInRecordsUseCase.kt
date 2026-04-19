package com.azuratech.azuratime.domain.checkin.usecase

import android.util.Log
import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to synchronize check-in records with the cloud.
 */
class SyncCheckInRecordsUseCase @Inject constructor(
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        val lastSync = sessionManager.getLastRecordsSyncTime()
        try {
            val syncResult = remoteDataSource.getRecordUpdates(schoolId, lastSync)
            if (syncResult is Result.Success) {
                val records = syncResult.data
                if (records.isNotEmpty()) {
                    records.forEach { record ->
                        localDataSource.insert(record)
                    }
                    sessionManager.saveLastRecordsSyncTime()
                    Log.i("SyncCheckInRecordsUseCase", "✅ Delta Sync: Pulled ${records.size} records")
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
