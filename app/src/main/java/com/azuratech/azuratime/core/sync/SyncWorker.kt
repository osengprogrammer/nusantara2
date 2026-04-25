package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuratime.domain.face.usecase.SyncFacesUseCase
import com.azuratech.azuratime.domain.checkin.usecase.SyncCheckInRecordsUseCase
import com.azuratech.azuratime.domain.classes.usecase.SyncClassesUseCase
import com.azuratech.azuratime.domain.assignment.usecase.SyncAssignmentsUseCase
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result as DomainResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🛡️ THE INVISIBLE GUARDRAIL: Persistent Background Sync
 * 
 * Canonical worker for all data synchronization.
 * Handles both OneTimeWork (manual) and PeriodicWork (background).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncFacesUseCase: SyncFacesUseCase,
    private val syncCheckInRecordsUseCase: SyncCheckInRecordsUseCase,
    private val syncClassesUseCase: SyncClassesUseCase,
    private val syncAssignmentsUseCase: SyncAssignmentsUseCase,
    private val syncUserUseCase: SyncUserUseCase,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: run {
            Log.w("AZURA_SYNC", "SyncWorker: No active school ID found. Aborting.")
            return@withContext Result.failure()
        }

        Log.d("AZURA_SYNC", "SyncWorker: Starting persistent background sync for school: $schoolId")

        // 1. Push & Sync Check-In Records (Local-First)
        when (val res = syncCheckInRecordsUseCase.invoke()) {
            is DomainResult.Failure -> {
                if (handleSyncError(res.error, "CheckInSync") == Result.retry()) {
                    return@withContext Result.retry()
                }
            }
            else -> Unit
        }

        // 2. Sync Faces (Pull Delta + Process Soft-Deletes)
        when (val res = syncFacesUseCase.invoke()) {
            is DomainResult.Failure -> {
                if (handleSyncError(res.error, "FaceSync") == Result.retry()) {
                    return@withContext Result.retry()
                }
            }
            else -> Unit
        }

        // 3. Modernized Sync (Classes, Users, Assignments)
        try {
            val currentUserId = sessionManager.getCurrentUserId()
            if (currentUserId != null) {
                syncUserUseCase(currentUserId)
            }
            syncClassesUseCase()
            syncAssignmentsUseCase()
        } catch (e: Exception) {
            Log.w("AZURA_SYNC", "UseCase sync failed: ${e.message}")
        }

        Log.d("AZURA_SYNC", "SyncWorker: Sync completed successfully.")
        Result.success()
    }

    /**
     * Maps AppError to WorkManager Result and logs the failure.
     */
    private fun handleSyncError(error: AppError, stage: String): Result {
        Log.e("AZURA_SYNC", "Sync Error at $stage: ${error.message}")
        return when (error) {
            is AppError.Network -> Result.retry()
            is AppError.LocalDB -> Result.failure()
            is AppError.BusinessRule -> Result.failure()
            is AppError.Unknown -> Result.retry()
        }
    }
}
