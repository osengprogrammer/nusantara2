package com.azuratech.azuratime.core.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.azuratech.azuratime.features.account.data.sync.AccountSyncWorker

/**
 * 🛠️ SYNC MANAGER
 * Centralized utility to enqueue and manage background sync workers.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueue a one-time account synchronization for the specified account.
     */
    fun enqueueAccountSync(accountId: String) {
        val data = workDataOf("accountId" to accountId)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<AccountSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            "sync_account_$accountId",
            ExistingWorkPolicy.REPLACE, // Replace to ensure latest local changes are prioritized
            request,
        )

        android.util.Log.d("SyncManager", "Enqueued account sync for user $accountId")
    }

    /**
     * Enqueue a sync for access requests (Join/Leave school).
     */
    fun enqueueAccessSync(accountId: String) {
        val data = workDataOf("accountId" to accountId)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AccessSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "sync_access_$accountId",
            ExistingWorkPolicy.REPLACE,
            request,
        )

        android.util.Log.d("SyncManager", "Enqueued access sync for user $accountId")
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
            request,
        )

        android.util.Log.d("SyncManager", "Enqueued school sync for school $schoolId")
    }

    /**
     * Enqueue a global synchronization for the current active school.
     */
    fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            "AZURA_SYNC_WORK",
            ExistingWorkPolicy.REPLACE,
            request,
        )

        android.util.Log.d("SyncManager", "Enqueued global manual sync (AZURA_SYNC_WORK)")
    }
}
