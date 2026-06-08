package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository

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
    private val schoolRepository: SchoolRepository,
    private val biometricRepository: BiometricRepository,
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AZURA_SYNC", "Starting background data synchronization...")

        // 1. Sync Biometrics (Cloud <-> Local)
        val biometricsResult = biometricRepository.syncBiometrics()
        if (biometricsResult is com.azuratech.azuraengine.result.Result.Failure) {
            return handleSyncError(biometricsResult.error, "Biometrics")
        }

        // 2. Sync Assignments (Cloud <-> Local)
        val assignmentsResult = biometricRepository.syncAssignments()
        if (assignmentsResult is com.azuratech.azuraengine.result.Result.Failure) {
            return handleSyncError(assignmentsResult.error, "Assignments")
        }

        // 3. Sync Attendance Records (Cloud <-> Local)
        val recordsResult = attendanceRepository.syncRecords()
        if (recordsResult is com.azuratech.azuraengine.result.Result.Failure) {
            return handleSyncError(recordsResult.error, "Records")
        }

        Log.i("AZURA_SYNC", "Successfully completed background data synchronization.")
        return Result.success()
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
