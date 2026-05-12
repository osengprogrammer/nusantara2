package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricFaceRepository
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 🔄 ACCESS SYNC WORKER
 * Synchronizes school access requests and ensures assignment integrity.
 * Follows SSOT pattern by delegating to Repositories.
 */
@HiltWorker
class AccessSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val faceRepository: BiometricFaceRepository,
    private val schoolRepository: SchoolRepository,
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "AccessSyncWorker"
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()

        Log.d(TAG, "Starting access sync for user $userId")

        return try {
            // 1. Push Access Requests (Join/Leave school)
            val accessResult = schoolRepository.pushAccessRequests(userId)
            if (accessResult is DomainResult.Failure) {
                Log.e(TAG, "Access requests sync failed: ${accessResult.error.message}")
                return handleSyncError(accessResult.error)
            }

            // 2. SSOT Integrity: Sync Assignments for the active school
            val assignmentResult = faceRepository.syncAssignments()
            if (assignmentResult is DomainResult.Failure) {
                Log.w(TAG, "Assignments sync failed: ${assignmentResult.error.message}")
            }

            // 3. Ensure Student identities exist for synced faces (Auto-Heal)
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isNotEmpty()) {
                studentRepository.autoHealStudentIdentities(schoolId)
            }

            Log.i(TAG, "Successfully completed access sync for user $userId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during access sync: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun handleSyncError(error: AppError): Result {
        return when (error) {
            is AppError.Network -> if (runAttemptCount < 3) Result.retry() else Result.failure()
            else -> Result.failure()
        }
    }
}
