package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.result.AppError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.session.domain.repository.SessionRepository

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
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AZURA_SYNC", "Starting background data synchronization...")

        var hasRetryableError = false

        // 1. Sync Biometrics (Cloud <-> Local)
        runCatching {
            val biometricsResult = biometricRepository.syncBiometrics()
            if (biometricsResult is com.azuratech.azuratime.core.result.Result.Failure) {
                Log.e("AZURA_SYNC", "Biometrics sync failed: ${biometricsResult.error.message}")
                if (biometricsResult.error is com.azuratech.azuratime.core.result.AppError.Network ||
                    biometricsResult.error is com.azuratech.azuratime.core.result.AppError.NetworkError) {
                    hasRetryableError = true
                }
            }
        }.onFailure { e ->
            Log.e("AZURA_SYNC", "Biometrics sync crashed: ${e.message}", e)
            hasRetryableError = true
        }

        // 2. Sync Assignments (Cloud <-> Local)
        runCatching {
            val assignmentsResult = biometricRepository.syncAssignments()
            if (assignmentsResult is com.azuratech.azuratime.core.result.Result.Failure) {
                Log.e("AZURA_SYNC", "Assignments sync failed: ${assignmentsResult.error.message}")
                if (assignmentsResult.error is com.azuratech.azuratime.core.result.AppError.Network ||
                    assignmentsResult.error is com.azuratech.azuratime.core.result.AppError.NetworkError) {
                    hasRetryableError = true
                }
            }
        }.onFailure { e ->
            Log.e("AZURA_SYNC", "Assignments sync crashed: ${e.message}", e)
            hasRetryableError = true
        }

        // 3. Sync Subjects (Cloud <-> Local)
        runCatching {
            val subjectsResult = sessionRepository.syncSubjects()
            if (subjectsResult is com.azuratech.azuratime.core.result.Result.Failure) {
                Log.e("AZURA_SYNC", "Subjects sync failed: ${subjectsResult.error.message}")
                if (subjectsResult.error is com.azuratech.azuratime.core.result.AppError.Network ||
                    subjectsResult.error is com.azuratech.azuratime.core.result.AppError.NetworkError) {
                    hasRetryableError = true
                }
            }
        }.onFailure { e ->
            Log.e("AZURA_SYNC", "Subjects sync crashed: ${e.message}", e)
            hasRetryableError = true
        }

        // 4. Sync Sessions (Cloud <-> Local)
        runCatching {
            val sessionsResult = sessionRepository.syncSessions()
            if (sessionsResult is com.azuratech.azuratime.core.result.Result.Failure) {
                Log.e("AZURA_SYNC", "Sessions sync failed: ${sessionsResult.error.message}")
                if (sessionsResult.error is com.azuratech.azuratime.core.result.AppError.Network ||
                    sessionsResult.error is com.azuratech.azuratime.core.result.AppError.NetworkError) {
                    hasRetryableError = true
                }
            }
        }.onFailure { e ->
            Log.e("AZURA_SYNC", "Sessions sync crashed: ${e.message}", e)
            hasRetryableError = true
        }

        // 5. Sync Attendance Records (Cloud <-> Local) — CRITICAL: always runs
        var attendanceFailed = false
        runCatching {
            val recordsResult = attendanceRepository.syncRecords()
            if (recordsResult is com.azuratech.azuratime.core.result.Result.Failure) {
                Log.e("AZURA_SYNC", "Attendance sync failed: ${recordsResult.error.message}")
                attendanceFailed = true
                if (recordsResult.error is com.azuratech.azuratime.core.result.AppError.Network ||
                    recordsResult.error is com.azuratech.azuratime.core.result.AppError.NetworkError) {
                    hasRetryableError = true
                }
            }
        }.onFailure { e ->
            Log.e("AZURA_SYNC", "Attendance sync crashed: ${e.message}", e)
            attendanceFailed = true
            hasRetryableError = true
        }

        // Evaluate final result
        return when {
            attendanceFailed -> {
                Log.w("AZURA_SYNC", "Attendance sync failed — scheduling retry.")
                Result.retry()
            }
            hasRetryableError -> {
                Log.w("AZURA_SYNC", "Non-attendance sync had retryable errors — retrying.")
                Result.retry()
            }
            else -> {
                Log.i("AZURA_SYNC", "Successfully completed background data synchronization.")
                Result.success()
            }
        }
    }

    /**
     * Maps AppError to WorkManager Result and logs the failure.
     */
    private fun handleSyncError(error: AppError, stage: String): Result {
        Log.e("AZURA_SYNC", "Sync Error at $stage: ${error.message}")
        return when (error) {
            is AppError.Network -> Result.retry()
            is AppError.NetworkError -> Result.retry()
            is AppError.LocalDB -> Result.failure()
            is AppError.LocalError -> Result.failure()
            is AppError.BusinessRule -> Result.failure()
            is AppError.Conflict -> {
                // Conflict during sync – log and treat as non‑fatal (skip this batch)
                Log.w("AZURA_SYNC", "Konflik data terdeteksi, skip.")
                Result.success()
            }
            is AppError.ValidationError -> Result.failure()
            is AppError.Unauthorized -> Result.failure()
            is AppError.Unknown -> Result.retry()
        }
    }
}
