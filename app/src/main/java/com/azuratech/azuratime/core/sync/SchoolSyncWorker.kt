package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.domain.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SchoolSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "SchoolSyncWorker"
    }

    override suspend fun doWork(): Result {
        val schoolId = inputData.getString("schoolId") ?: return Result.failure()
        val dao = database.schoolClassDao()

        return try {
            val school = dao.getSchoolById(schoolId)
            if (school == null) {
                Log.e(TAG, "School $schoolId not found in local DB")
                return Result.failure()
            }

            if (school.syncStatus == SyncStatus.SYNCED.name) {
                return Result.success()
            }

            Log.d(TAG, "Syncing school $schoolId (Status: ${school.syncStatus})")

            val schoolData = mutableMapOf(
                "schoolId" to school.id,
                "ownerId" to school.accountId,
                "schoolName" to school.name,
                "timezone" to school.timezone,
                "status" to school.status,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection("schools").document(schoolId)

            when (school.syncStatus) {
                SyncStatus.PENDING_INSERT.name -> {
                    schoolData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                    docRef.set(schoolData).await()
                }
                SyncStatus.PENDING_UPDATE.name, SyncStatus.PENDING_DELETE.name -> {
                    docRef.update(schoolData).await()
                }
            }

            // Success: Mark as synced
            dao.upsertSchool(school.copy(syncStatus = SyncStatus.SYNCED.name))
            
            Log.i(TAG, "Successfully synced school $schoolId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for school $schoolId: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
