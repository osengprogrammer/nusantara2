package com.azuratech.azuratime.core.domain.repository

import android.content.Context
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    fun getCurrentUid(): Result<String?>
    fun getCurrentEmail(): Result<String>
    suspend fun initializeAiBrain(context: Context): Result<Unit>
    fun observeRevokeStatus(uid: String): Flow<Result<Boolean>>
    fun executeRevocationCleanup(): Result<Unit>
}
