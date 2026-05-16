package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import kotlinx.coroutines.flow.Flow

interface BiometricLocalDataSource {
    fun getAllStudentsFlow(schoolId: String): Flow<List<StudentBiometricEntity>>
    fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<StudentBiometricDetails>>
    fun getAllStudentsForScanningFlow(schoolId: String): Flow<List<StudentBiometricEntity>>
    fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<List<StudentBiometricEntity>>
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>
    fun getAllAssignmentsFlow(schoolId: String): Flow<List<StudentClassAssignmentEntity>>
    suspend fun getStudentWithDetails(studentId: String, schoolId: String): StudentBiometricDetails?
    suspend fun getClassIdsForStudent(studentId: String, schoolId: String): List<String>
    suspend fun getAllStudentsForScanningList(schoolId: String): List<StudentBiometricEntity>
    suspend fun getStudentFaceById(studentId: String, schoolId: String): StudentBiometricEntity?
    suspend fun getStudentFaceByIdentity(studentId: String, schoolId: String): StudentBiometricEntity?
    suspend fun upsertStudentFace(studentFace: StudentBiometricEntity)
    suspend fun upsertAllStudentFaces(studentFaces: List<StudentBiometricEntity>)
    suspend fun deleteStudentFaceById(studentId: String, schoolId: String)
    suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String)
    suspend fun markPendingDeletion(studentId: String, schoolId: String)
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity)
}
