package com.azuratech.azuratime.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repository.UserRepository
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.repository.FaceAssignmentRepository
import com.azuratech.azuratime.data.repository.CheckInRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase,
    private val userRepository: UserRepository,
    private val faceRepository: FaceRepository,
    private val faceAssignmentRepository: FaceAssignmentRepository,
    private val classRepository: ClassRepository,
    private val checkInRepository: CheckInRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val schoolId = sessionManager.getActiveSchoolId()
        if (schoolId.isNullOrEmpty()) return Result.success()

        return try {
            Log.d("AzuraSync", "🔄 Background Sync Started: $schoolId")

            // 1. PUSH PHASE: Upload local changes to cloud
            val unsyncedRecords = database.checkInRecordDao().getUnsyncedRecords(schoolId)
            for (record in unsyncedRecords) {
                try {
                    checkInRepository.saveRecord(record)
                } catch (e: Exception) {
                    Log.e("AzuraSync", "Push Record Failed: ${record.id}")
                }
            }

            // 2. PULL PHASE: Delta sync from cloud to local Room
            userRepository.syncUserFromCloud(sessionManager.getCurrentUserId() ?: "")
            classRepository.performClassDeltaSync()
            faceRepository.performFaceDeltaSync()
            faceAssignmentRepository.performAssignmentSync()
            checkInRepository.performRecordsDeltaSync()

            Log.d("AzuraSync", "✅ Background Sync Completed Successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("AzuraSync", "❌ Background Sync Failed: ${e.message}")
            Result.retry()
        }
    }
}