package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.staff.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import kotlinx.coroutines.flow.Flow

interface FaceLocalDataSource {
    fun getAllFacesFlow(schoolId: String): Flow<List<BiometricFaceEntity>>
    fun getAllFacesWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>>
    fun getAllFacesForScanningFlow(schoolId: String): Flow<List<BiometricFaceEntity>>
    fun getFacesInClassFlow(classId: String, schoolId: String): Flow<List<BiometricFaceEntity>>
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>
    fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>>
    suspend fun getFaceWithDetails(faceId: String, schoolId: String): FaceWithDetails?
    suspend fun getClassIdsForFace(faceId: String, schoolId: String): List<String>
    suspend fun getAllFacesForScanningList(schoolId: String): List<BiometricFaceEntity>
    suspend fun getFaceById(faceId: String, schoolId: String): BiometricFaceEntity?
    suspend fun getFaceByStudentId(studentId: String, schoolId: String): BiometricFaceEntity?
    suspend fun upsertFace(face: BiometricFaceEntity)
    suspend fun upsertAll(faces: List<BiometricFaceEntity>)
    suspend fun deleteFace(face: BiometricFaceEntity)
    suspend fun deleteFaceById(faceId: String, schoolId: String)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity)
    suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String)
    suspend fun markPendingDeletion(faceId: String, schoolId: String)
}
