package com.azuratech.azuratime.features.account.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
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
    private val schoolRepository: SchoolRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AccountSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🚀 Starting Account Sync...")
        val accountId = inputData.getString("accountId") ?: return Result.failure()

        return try {
            // 1. Sync Account Profile (Cloud -> Local)
            val accountResult = accountRepository.syncAccount(accountId)
            if (accountResult is com.azuratech.azuraengine.result.Result.Failure) {
                return handleSyncError(accountResult.error)
            }

            // 2. Push Pending Local Changes (Local -> Cloud)
            val pushResult = accountRepository.pushAccount(accountId)
            if (pushResult is com.azuratech.azuraengine.result.Result.Failure) {
                return handleSyncError(pushResult.error)
            }

            // 3. Sync Workspaces (Schools) for this account
            val account = (accountResult as com.azuratech.azuraengine.result.Result.Success).data
            val schoolIds = account.memberships.keys.toList()
            schoolRepository.syncSchools(schoolIds)

            // 4. 🔥 NEW: Explicitly sync classes for each school membership
            schoolIds.forEach { schoolId ->
                schoolRepository.syncClasses(accountId, schoolId)
            }

            // 5. Sync Students & Biometrics (Background Auto-Heal)
            account.activeSchoolId?.let { schoolId ->
                studentRepository.pullStudents(schoolId)
                biometricRepository.syncBiometrics()
                studentRepository.autoHealStudentIdentities(schoolId)
            }

            Log.d(TAG, "✅ Account Sync Completed Successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical Sync Error: ${e.message}")
            Result.failure()
        }
    }

    private fun handleSyncError(error: AppError): Result {
        return when (error) {
            is AppError.Network -> if (runAttemptCount < 3) Result.retry() else Result.failure()
            else -> Result.failure()
        }
    }
}
