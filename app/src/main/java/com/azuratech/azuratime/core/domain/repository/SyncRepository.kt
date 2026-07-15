package com.azuratech.azuratime.core.domain.repository

import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val isSyncingFlow: StateFlow<Result<Boolean>>
    fun forceSyncFromCloud(): Result<Unit>
}
