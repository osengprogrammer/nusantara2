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
class AccessSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "AccessSyncWorker"
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val dao = database.accessRequestDao()

        return try {
            val unsynced = dao.getUnsyncedRequestsByUser(userId)
            if (unsynced.isEmpty()) return Result.success()

            Log.d(TAG, "Syncing ${unsynced.size} access requests for user $userId")

            for (request in unsynced) {
                val requestData = mapOf(
                    "requestId" to request.requestId,
                    "requesterId" to request.requesterId,
                    "schoolId" to request.schoolId,
                    "schoolName" to request.schoolName,
                    "status" to request.status.name,
                    "createdAt" to request.createdAt,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                firestore.collection("access_requests")
                    .document(request.requestId)
                    .set(requestData)
                    .await()

                // Mark as synced locally
                dao.insertRequest(request.copy(
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            
            Log.i(TAG, "Successfully synced access requests for user $userId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Access sync failed for user $userId: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
