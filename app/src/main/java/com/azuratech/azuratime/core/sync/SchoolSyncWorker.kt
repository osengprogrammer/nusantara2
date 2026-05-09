package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.data.repo.SchoolRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 🔄 SCHOOL SYNC WORKER
 * Synchronizes school metadata from local Room to Firestore using SchoolRepository.
 */
@HiltWorker
class SchoolSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val schoolRepository: SchoolRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "SchoolSyncWorker"
    }

    override suspend fun doWork(): Result {
        val schoolId = inputData.getString("schoolId") ?: return Result.failure()

        Log.d(TAG, "Starting sync for school $schoolId")

        return when (val result = schoolRepository.pushSchool(schoolId)) {
            is DomainResult.Success -> {
                Log.i(TAG, "Successfully synced school $schoolId")
                Result.success()
            }
            is DomainResult.Failure -> {
                Log.e(TAG, "Sync failed for school $schoolId: ${result.error.message}")
                handleSyncError(result.error)
            }
            else -> Result.retry()
        }
    }

    private fun handleSyncError(error: AppError): Result {
        return when (error) {
            is AppError.Network -> if (runAttemptCount < 3) Result.retry() else Result.failure()
            else -> Result.failure()
        }
    }
}
