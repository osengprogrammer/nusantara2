package com.azuratech.azuratime.features.biometric.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import kotlinx.coroutines.flow.Flow

/**
 * 🧬 BIOMETRIC REPOSITORY INTERFACE (v3.2.0-ai-native)
 */
interface BiometricRepository {
    /**
     * Observe all biometric enrollments for the current school.
     */
    fun observeEnrollments(): Flow<List<BiometricEnrollmentProfile>>

    /**
     * 🔥 One-shot enroll: Save student biometric embedding.
     */
    suspend fun enrollStudent(studentId: String, embedding: FloatArray): Result<Unit>

    /**
     * 🔥 Capture face: Placeholder for hardware-level capture if applicable,
     * or a way to return mock data in tests.
     */
    suspend fun captureFace(): Result<FloatArray>

    /**
     * Delete biometric enrollment for a student.
     */
    suspend fun deleteEnrollment(studentId: String): Result<Unit>

    /**
     * Sync biometrics with cloud.
     */
    suspend fun syncBiometrics(): Result<Unit>
}
