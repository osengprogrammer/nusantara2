package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to synchronize check-in records with the cloud.
 * Performs both Push (local changes to cloud) and Pull (remote changes to local).
 * 🔥 Clean Architecture compliant.
 */
class SyncCheckInRecordsUseCase @Inject constructor(
    private val repository: CheckInRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        // 1. PUSH PHASE: Upload local changes to cloud
        try {
            val unsyncedRecords = repository.getUnsyncedRecords(schoolId)
            for (record in unsyncedRecords) {
                val syncRes = repository.syncRecord(record)
                if (syncRes is Result.Failure) {
                    if (syncRes.error is AppError.Network) {
                        return@withContext Result.Failure(syncRes.error)
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR: [SyncCheckInRecordsUseCase] Error during push phase: ${e.message}")
        }

        // 2. PULL PHASE: Delta sync from cloud to local
        val lastSync = sessionManager.getLastRecordsSyncTime()
        try {
            val syncResult = repository.getRecordUpdates(schoolId, lastSync)
            if (syncResult is Result.Success) {
                val records = syncResult.data
                if (records.isNotEmpty()) {
                    records.forEach { record ->
                        repository.saveRecord(record)
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
