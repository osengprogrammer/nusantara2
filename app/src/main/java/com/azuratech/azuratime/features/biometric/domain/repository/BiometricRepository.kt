package com.azuratech.azuratime.features.biometric.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.biometric.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow

/**
 * 🧬 BIOMETRIC REPOSITORY INTERFACE (v3.2.0-ai-native)
 */
interface BiometricRepository {
    /**
     * Observe all biometric enrollments for the current school.
     */
    fun observeEnrollmentsFlow(): Flow<Result<List<BiometricEnrollmentProfile>>>

    /**
     * 🔥 One-shot enroll: Save student biometric embedding.
     */
    suspend fun enrollStudent(studentId: String, embedding: FloatArray): Result<Unit>

    /**
     * 🔥 Capture face: Placeholder for hardware-level capture if applicable.
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

    // --- Legacy & Extended Methods used by ViewModels ---
    fun getAllStudentsFlow(schoolId: String): Flow<Result<List<StudentBiometricEntity>>>
    fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<Result<List<StudentBiometricEntity>>>
    suspend fun updateStudentClass(studentId: String, classId: String?): Result<Unit>
    suspend fun saveStudentProfile(profile: StudentProfile, photoBytes: ByteArray? = null): Result<Unit>
    suspend fun syncAssignments(): Result<Unit>
    suspend fun removeAllAssignmentsForStudent(studentId: String): Result<Unit>
    suspend fun assignStudentToClass(studentId: String, classId: String): Result<Unit>
    suspend fun removeStudentFromClass(studentId: String, classId: String): Result<Unit>
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity): Result<Unit>
    suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String): Result<Unit>
    suspend fun getStudentWithDetails(studentId: String, schoolId: String): Result<com.azuratech.azuratime.core.data.local.StudentBiometricDetails?>
    suspend fun getClassIdsForStudent(studentId: String, schoolId: String): Result<List<String>>
    fun getEnrolledStudentsFlow(schoolId: String): Flow<Result<List<StudentBiometricEntity>>>
    fun getStudentsWithDetailsFlow(schoolId: String): Flow<Result<List<com.azuratech.azuratime.core.data.local.StudentBiometricDetails>>>
    fun getAllAssignmentsFlow(schoolId: String): Flow<Result<List<StudentClassAssignmentEntity>>>
    suspend fun upsertStudentBiometric(studentBiometric: StudentBiometricEntity): Result<Unit>
}
