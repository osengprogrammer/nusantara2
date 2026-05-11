package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repo.BiometricFaceRepository
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.student.repository.StudentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 🔄 PROFILE SYNC WORKER
 * Synchronizes user profile and associated student data from local Room to Firestore.
 * Follows SSOT pattern by delegating to Repositories.
 */
@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val faceRepository: BiometricFaceRepository,
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ProfileSyncWorker"
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()

        Log.d(TAG, "Starting sync for profile: $userId")

        return try {
            // 1. Push User Profile updates
            val userResult = userRepository.pushUser(userId)
            if (userResult is DomainResult.Failure) {
                Log.e(TAG, "User profile sync failed: ${userResult.error.message}")
                return handleSyncError(userResult.error)
            }

            // 2. Push Student Profiles (Faces + Assignments) for the active school
            val studentResult = studentRepository.pushPendingProfiles()
            if (studentResult is DomainResult.Failure) {
                Log.e(TAG, "Student profiles sync failed: ${studentResult.error.message}")
                // We don't necessarily want to fail the whole worker if students fail, 
                // but for consistency we'll retry if it's a network error.
                return handleSyncError(studentResult.error)
            }

            // 3. Sync Faces (Pull updates)
            val faceResult = faceRepository.syncFaces()
            if (faceResult is DomainResult.Failure) {
                Log.w(TAG, "Face sync (pull) failed: ${faceResult.error.message}")
            }

            Log.i(TAG, "Successfully synced profile and student data for user $userId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during profile sync: ${e.message}")
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
