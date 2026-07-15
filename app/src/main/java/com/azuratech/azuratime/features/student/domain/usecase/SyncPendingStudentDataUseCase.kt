package com.azuratech.azuratime.features.student.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import javax.inject.Inject

/**
 * 🔄 SYNC PENDING STUDENT DATA USE CASE
 * Coordinates student data sync across repositories to avoid circular dependencies.
 * Callers (ViewModels / Workers) use this instead of calling both repositories directly
 * when they need to push/pull student data AND biometric/assignment data together.
 */
class SyncPendingStudentDataUseCase @Inject constructor(
    private val studentRepository: StudentRepository,
    private val biometricRepository: BiometricRepository,
) {
    /**
     * Push all unsynced local data (students + biometrics) to cloud.
     */
    suspend fun pushAll(schoolId: String): Result<Unit> {
        // 1. Push student profiles to Firestore
        val studentResult = studentRepository.pushPendingProfiles()

        // 2. Push pending biometrics and assignments to cloud
        val biometricResult = biometricRepository.syncPendingBiometricsToCloud(schoolId)

        return if (studentResult is Result.Success && biometricResult is Result.Success) {
            Result.Success(Unit)
        } else {
            studentResult
        }
    }

    /**
     * Pull student data and assignments from cloud to local.
     */
    suspend fun pullAll(schoolId: String): Result<Unit> {
        // 1. Pull student identities from Firestore
        val studentResult = studentRepository.pullStudents(schoolId)

        // 2. Pull remote assignments and save locally
        biometricRepository.pullAssignmentsFromCloud(schoolId)

        return studentResult
    }
}
