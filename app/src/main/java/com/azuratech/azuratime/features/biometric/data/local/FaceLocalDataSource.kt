package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import kotlinx.coroutines.flow.Flow

interface FaceLocalDataSource {
    fun getAllStudentsFlow(schoolId: String): Flow<List<BiometricFaceEntity>>
    fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>>
    fun getAllStudentsForScanningFlow(schoolId: String): Flow<List<BiometricFaceEntity>>
    fun getStudentsInClassFlow(classId: String, schoolId: String): Flow<List<BiometricFaceEntity>>
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>
    fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>>
    suspend fun getStudentWithDetails(studentId: String, schoolId: String): FaceWithDetails?
    suspend fun getClassIdsForStudent(studentId: String, schoolId: String): List<String>
    suspend fun getAllStudentsForScanningList(schoolId: String): List<BiometricFaceEntity>
    suspend fun getStudentFaceById(studentId: String, schoolId: String): BiometricFaceEntity?
    suspend fun getStudentFaceByIdentity(studentId: String, schoolId: String): BiometricFaceEntity?
    suspend fun upsertStudentFace(studentFace: BiometricFaceEntity)
    suspend fun upsertAllStudentFaces(studentFaces: List<BiometricFaceEntity>)
    suspend fun deleteStudentFace(studentFace: BiometricFaceEntity)
    suspend fun deleteStudentFaceById(studentId: String, schoolId: String)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity)
    suspend fun deleteAssignmentsByStudent(studentId: String, schoolId: String)
    suspend fun markPendingDeletion(studentId: String, schoolId: String)
}
