package com.azuratech.azuratime.ui.sync

import android.content.Context
import android.widget.Toast
import androidx.work.*
import com.azuratech.azuratime.core.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("AZURA_SYNC_WORK")
                .collectLatest { workInfos ->
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    val previouslySyncing = _isSyncing.value
                    
                    _isSyncing.value = isRunning

                    // Jika transisi dari RUNNING ke SUCCEEDED (dan bukan karena ada task baru di-enqueue)
                    val finishedNow = workInfos.any { it.state == WorkInfo.State.SUCCEEDED }
                    if (previouslySyncing && !isRunning && finishedNow) {
                        Toast.makeText(context, "Data Updated", Toast.LENGTH_SHORT).show()
                        workManager.pruneWork()
                    }
                }
        }
    }

    fun forceSyncFromCloud(onComplete: (() -> Unit)? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "AZURA_SYNC_WORK",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
        
        onComplete?.invoke()
    }
}