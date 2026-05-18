package com.azuratech.azuratime.features.student.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import kotlinx.coroutines.flow.Flow

interface StudentRegistrationRepository {
    suspend fun getAllBiometrics(schoolId: String): Result<List<StudentBiometricEntity>>
    suspend fun upsertBiometric(biometric: StudentBiometricEntity): Result<Unit>
    suspend fun upsertAllBiometrics(biometrics: List<StudentBiometricEntity>): Result<Unit>
    suspend fun getClassByName(name: String): Result<ClassEntity?>
    suspend fun insertClass(classEntity: ClassEntity): Result<Unit>
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity): Result<Unit>
    fun processCsv(uri: String, dataType: String): Flow<Result<com.azuratech.azuraengine.model.ProcessResult>>
}
