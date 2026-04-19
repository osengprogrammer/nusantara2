package com.azuratech.azuratime.data.local

import kotlinx.coroutines.flow.Flow

interface FaceLocalDataSource {
    fun getAllFacesFlow(schoolId: String): Flow<List<FaceEntity>>
    fun getAllFacesForScanningFlow(schoolId: String): Flow<List<FaceEntity>>
    fun getFacesInClassFlow(classId: String, schoolId: String): Flow<List<FaceEntity>>
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>
    fun getAllAssignmentsFlow(schoolId: String): Flow<List<FaceAssignmentEntity>>
    suspend fun getFaceWithDetails(faceId: String, schoolId: String): FaceWithDetails?
    suspend fun getClassIdsForFace(faceId: String, schoolId: String): List<String>
    suspend fun getAllFacesForScanningList(schoolId: String): List<FaceEntity>
    suspend fun getFaceById(faceId: String, schoolId: String): FaceEntity?
    suspend fun upsertFace(face: FaceEntity)
    suspend fun upsertAll(faces: List<FaceEntity>)
    suspend fun deleteFace(face: FaceEntity)
    suspend fun deleteFaceById(faceId: String, schoolId: String)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity)
    suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String)
    suspend fun markPendingDeletion(faceId: String, schoolId: String)
}
