package com.azuratech.azuratime.features.account.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.Result as DomainResult
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 🔄 ACCOUNT SYNC WORKER (v3.2.0-ai-native)
 * Synchronizes account profile and associated student data.
 * Follows SSOT pattern by delegating to Repositories.
 */
@HiltWorker
class AccountSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val studentRepository: StudentRepository,
    private val biometricRepository: BiometricRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AccountSyncWorker"
    }

    override suspend fun doWork(): Result {
        val accountId = inputData.getString("accountId") ?: return Result.failure()

        Log.d(TAG, "Starting sync for account: $accountId")

        return try {
            // 1. Pull latest Account from Cloud to Room (Local-First SSOT)
            val syncResult = accountRepository.syncAccount(accountId)
            if (syncResult is DomainResult.Failure) {
                Log.e(TAG, "Account pull sync failed: ${syncResult.error.message}")
                return handleSyncError(syncResult.error)
            }

            // 2. Push any local Account changes to Cloud
            val pushResult = accountRepository.pushAccount(accountId)
            if (pushResult is DomainResult.Failure) {
                Log.e(TAG, "Account push sync failed: ${pushResult.error.message}")
                return handleSyncError(pushResult.error)
            }

            // 3. Push Student Profiles (Biometrics + Assignments) for the active school
            val studentResult = studentRepository.pushPendingProfiles()
            if (studentResult is DomainResult.Failure) {
                Log.e(TAG, "Student profiles sync failed: ${studentResult.error.message}")
                return handleSyncError(studentResult.error)
            }

            // 4. Sync Biometrics (Pull updates)
            val biometricResult = biometricRepository.syncBiometrics()
            if (biometricResult is DomainResult.Failure) {
                Log.w(TAG, "Biometric sync (pull) failed: ${biometricResult.error.message}")
            }

            Log.i(TAG, "Successfully synced account and student data for $accountId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during account sync: ${e.message}")
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
