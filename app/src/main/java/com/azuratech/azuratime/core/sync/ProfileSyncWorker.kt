package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * 🔄 PROFILE SYNC WORKER
 * Synchronizes user memberships and profile data from local Room to Firestore.
 */
@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ProfileSyncWorker"
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()

        return try {
            val user = userRepository.getUserDao().getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User $userId not found in local DB")
                return Result.failure()
            }

            // If already synced, we're done
            if (user.syncStatus == SyncStatus.SYNCED.name) {
                return Result.success()
            }

            Log.d(TAG, "Starting sync for user $userId")

            // Map memberships to Firestore format
            // In our current schema, we push the whole memberships map
            val membershipsData = user.memberships.mapValues { (_, membership) ->
                mapOf(
                    "schoolName" to membership.schoolName,
                    "role" to membership.role,
                    "assignedClassIds" to membership.assignedClassIds
                )
            }

            val updateData = mapOf(
                "memberships" to membershipsData,
                "activeSchoolId" to user.activeSchoolId,
                "status" to user.status,
                "isActive" to user.isActive,
                "activeClassId" to user.activeClassId,
                "role" to user.role,
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Push to Firestore
            firestore.collection("whitelisted_users").document(userId)
                .update(updateData)
                .await()

            // Success: Mark as synced in Room
            userRepository.markUserSynced(userId)

            Log.i(TAG, "Successfully synced profile for user $userId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for user $userId: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
