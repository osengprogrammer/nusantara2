package com.azuratech.azuratime.core.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🛠️ SYNC MANAGER
 * Centralized utility to enqueue and manage background sync workers.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueue a one-time profile synchronization for the specified user.
     */
    fun enqueueProfileSync(userId: String) {
        val data = workDataOf("userId" to userId)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "sync_profile_$userId",
            ExistingWorkPolicy.REPLACE, // Replace to ensure latest local changes are prioritized
            request
        )
        
        android.util.Log.d("SyncManager", "Enqueued profile sync for user $userId")
    }

    /**
     * Enqueue a sync for access requests (Join/Leave school).
     */
    fun enqueueAccessSync(userId: String) {
        val data = workDataOf("userId" to userId)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AccessSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "sync_access_$userId",
            ExistingWorkPolicy.REPLACE,
            request
        )
        
        android.util.Log.d("SyncManager", "Enqueued access sync for user $userId")
    }

    /**
     * Enqueue a sync for school metadata (Create/Update school).
     */
    fun enqueueSchoolSync(schoolId: String) {
        val data = workDataOf("schoolId" to schoolId)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SchoolSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "sync_school_$schoolId",
            ExistingWorkPolicy.REPLACE,
            request
        )
        
        android.util.Log.d("SyncManager", "Enqueued school sync for school $schoolId")
    }
}
