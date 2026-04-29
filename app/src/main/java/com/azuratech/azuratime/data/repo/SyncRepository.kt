package com.azuratech.azuratime.data.repo

import android.content.Context
import androidx.work.*
import com.azuratech.azuratime.core.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("AZURA_SYNC_WORK")
                .collectLatest { workInfos ->
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    _isSyncing.value = isRunning
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
