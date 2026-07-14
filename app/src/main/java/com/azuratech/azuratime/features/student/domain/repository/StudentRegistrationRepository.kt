package com.azuratech.azuratime.features.student.domain.repository

import com.azuratech.azuraengine.model.ProcessResult
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.core.data.local.ClassEntity
import kotlinx.coroutines.flow.Flow

/**
 * 🏰 STUDENT REGISTRATION REPOSITORY (v3.2.0-ai-native)
 * Unified interface for student registration operations.
 */
interface StudentRegistrationRepository {
    suspend fun getAllBiometrics(schoolId: String): Result<List<StudentBiometricEntity>>
    suspend fun upsertBiometric(biometric: StudentBiometricEntity): Result<Unit>
    suspend fun upsertAllBiometrics(biometrics: List<StudentBiometricEntity>): Result<Unit>

    suspend fun getClassByName(name: String): Result<ClassEntity?>
    suspend fun insertClass(classEntity: ClassEntity): Result<Unit>
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity): Result<Unit>

    /**
     * 🔥 THE BULK ENGINE: Process CSV file and emit progress/results.
     */
    fun processCsvFlow(uri: String, schoolId: String): Flow<Result<ProcessResult>>
}
