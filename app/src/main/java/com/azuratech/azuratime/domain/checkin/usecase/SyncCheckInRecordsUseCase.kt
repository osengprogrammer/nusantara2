package com.azuratech.azuratime.domain.checkin.usecase

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
 * Performs both Push (local changes to cloud) and Pull (remote changes to local).
 */
class SyncCheckInRecordsUseCase @Inject constructor(
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        // 1. PUSH PHASE: Upload local changes to cloud
        try {
            val unsyncedRecords = localDataSource.getUnsyncedRecords(schoolId)
            for (record in unsyncedRecords) {
                val syncRes = remoteDataSource.syncRecord(record)
                if (syncRes is Result.Success) {
                    localDataSource.update(record.copy(isSynced = true))
                } else if (syncRes is Result.Failure) {
                    // If it's a network error, we might want to stop early and retry later
                    if (syncRes.error is AppError.Network) {
                        return@withContext Result.Failure(syncRes.error)
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR: [SyncCheckInRecordsUseCase] Error during push phase: ${e.message}")
            // Continue to pull phase even if push fails, unless it's a critical error
        }

        // 2. PULL PHASE: Delta sync from cloud to local Room
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
                    println("[SyncCheckInRecordsUseCase] ✅ Delta Sync: Pulled ${records.size} records")
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
