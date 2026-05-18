package com.azuratech.azuratime.core.data.repo

import android.content.Context
import androidx.work.*
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.repository.SyncRepository
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
class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncRepository {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val workManager = WorkManager.getInstance(context)

    private val _isSyncingFlow = MutableStateFlow<Result<Boolean>>(Result.Success(false))
    override val isSyncingFlow: StateFlow<Result<Boolean>> = _isSyncingFlow.asStateFlow()

    init {
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("AZURA_SYNC_WORK")
                .collectLatest { workInfos ->
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    _isSyncingFlow.value = Result.Success(isRunning)
                }
        }
    }

    override fun forceSyncFromCloud(): Result<Unit> {
        return try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                "AZURA_SYNC_WORK",
                ExistingWorkPolicy.REPLACE,
                syncRequest,
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.BusinessRule(e.message))
        }
    }
}
