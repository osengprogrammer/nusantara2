package com.azuratech.azuratime.features.biometric.data.remote

import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuraengine.result.Result

interface BiometricRemoteDataSource {
    suspend fun getBiometricUpdates(schoolId: String, lastSync: Long): Result<List<Pair<StudentBiometricEntity, Boolean>>>
    suspend fun uploadBiometricPhoto(schoolId: String, studentId: String, imageBytes: ByteArray): Result<String?>
    suspend fun bulkSyncBiometrics(schoolId: String, students: List<StudentBiometricEntity>): Result<Unit>
    suspend fun syncStudentAssignment(assignment: StudentClassAssignmentEntity): Result<Unit>
    suspend fun deleteStudent(studentId: String, schoolId: String, classIds: List<String>): Result<Unit>
    suspend fun deleteStudentAssignments(assignment: StudentClassAssignmentEntity): Result<Unit>
    suspend fun getStudentAssignments(schoolId: String): Result<List<StudentClassAssignmentEntity>>
}
