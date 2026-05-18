package com.azuratech.azuratime.core.ui.sync

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.azuratech.azuratime.core.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val isSyncing: Boolean = false,
)

sealed class SyncUiEvent {
    data class ForceSync(val onComplete: (() -> Unit)? = null) : SyncUiEvent()
}

/**
 * 🚀 SYNC VIEW MODEL (v3.2.0-ai-native)
 * Manages background sync operations with WorkManager.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(SyncUiState())
    val uiStateFlow: StateFlow<SyncUiState> = _uiStateFlow.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("AZURA_SYNC_WORK")
                .collectLatest { workInfos ->
                    val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    val previouslySyncing = _uiStateFlow.value.isSyncing

                    _uiStateFlow.update { it.copy(isSyncing = isRunning) }

                    val finishedNow = workInfos.any { it.state == WorkInfo.State.SUCCEEDED }
                    if (previouslySyncing && !isRunning && finishedNow) {
                        Toast.makeText(context, "Data Updated", Toast.LENGTH_SHORT).show()
                        workManager.pruneWork()
                    }
                }
        }
    }

    fun onEvent(event: SyncUiEvent) {
        when (event) {
            is SyncUiEvent.ForceSync -> forceSyncFromCloud(event.onComplete)
        }
    }

    private fun forceSyncFromCloud(onComplete: (() -> Unit)? = null) {
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

        onComplete?.invoke()
    }
}
