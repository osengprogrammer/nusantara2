package com.azuratech.azuratime.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result as DomainResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.BiometricFaceRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.attendance.domain.repository.CheckInRepository
import com.azuratech.azuratime.data.repo.StaffAccountRepository

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
    private val faceRepository: BiometricFaceRepository,
    private val studentRepository: StudentRepository,
    private val checkInRepository: CheckInRepository,
    private val userRepository: StaffAccountRepository,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: run {
            Log.w("AZURA_SYNC", "SyncWorker: No active school ID found. Aborting.")
            return@withContext Result.failure()
        }

        Log.d("AZURA_SYNC", "SyncWorker: Starting persistent background sync for school: $schoolId")

        // 1. Push & Sync Check-In Records (Local-First)
        val checkInResult = checkInRepository.syncRecords()
        if (checkInResult is DomainResult.Failure) {
            if (handleSyncError(checkInResult.error, "CheckInSync") == Result.retry()) {
                return@withContext Result.retry()
            }
        }

        // 2. Push Student Profiles (Faces + Assignments)
        val pushResult = studentRepository.pushPendingProfiles()
        if (pushResult is DomainResult.Failure) {
            if (handleSyncError(pushResult.error, "PushStudents") == Result.retry()) {
                return@withContext Result.retry()
            }
        }

        // 3. Sync Faces (Pull Delta + Process Soft-Deletes)
        val faceResult = faceRepository.syncFaces()
        if (faceResult is DomainResult.Failure) {
            if (handleSyncError(faceResult.error, "FaceSync") == Result.retry()) {
                return@withContext Result.retry()
            }
        } else {
            // 🔥 SSOT Auto-Healing: Ensure Student identities exist for all synced faces
            studentRepository.autoHealStudentIdentities(schoolId)
        }

        // 4. Modernized Sync (Classes, Users, Assignments)
        try {
            val currentUserId = sessionManager.getCurrentUserId() ?: ""
            if (currentUserId.isNotEmpty()) {
                userRepository.syncUser(currentUserId)
            }
            schoolRepository.syncClasses(currentUserId, schoolId)
            faceRepository.syncAssignments()
        } catch (e: Exception) {
            Log.w("AZURA_SYNC", "Repository sync failed: ${e.message}")
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
