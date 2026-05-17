package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 🔄 PROFILE SYNC WORKER
 * Synchronizes account profile and associated student data from local Room to Firestore.
 * Follows SSOT pattern by delegating to Repositories.
 */
@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val biometricRepository: BiometricRepository,
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ProfileSyncWorker"
    }

    override suspend fun doWork(): Result {
        val accountId = inputData.getString("accountId") ?: return Result.failure()

        Log.d(TAG, "Starting sync for account: $accountId")

        return try {
            // 1. Push Account Profile updates
            val accountResult = accountRepository.pushAccount(accountId)
            if (accountResult is DomainResult.Failure) {
                Log.e(TAG, "Account profile sync failed: ${accountResult.error.message}")
                return handleSyncError(accountResult.error)
            }

            // 2. Push Student Profiles (Biometrics + Assignments) for the active school
            val studentResult = studentRepository.pushPendingProfiles()
            if (studentResult is DomainResult.Failure) {
                Log.e(TAG, "Student profiles sync failed: ${studentResult.error.message}")
                return handleSyncError(studentResult.error)
            }

            // 3. Sync Biometrics (Pull updates)
            val biometricResult = biometricRepository.syncBiometrics()
            if (biometricResult is DomainResult.Failure) {
                Log.w(TAG, "Biometric sync (pull) failed: ${biometricResult.error.message}")
            }

            Log.i(TAG, "Successfully synced profile and student data for account $accountId")
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
