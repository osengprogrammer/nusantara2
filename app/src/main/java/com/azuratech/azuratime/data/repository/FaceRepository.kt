package com.azuratech.azuratime.data.repository

import android.app.Application
import android.graphics.Bitmap
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.face.usecase.*
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    // Delegation methods for DAOs / DataSources
    fun getAllFacesFlow(schoolId: String) = localDataSource.getAllFacesFlow(schoolId)
    fun getAllFacesForScanningFlow(schoolId: String) = localDataSource.getAllFacesForScanningFlow(schoolId)
    fun getFacesInClassFlow(classId: String, schoolId: String) = localDataSource.getFacesInClassFlow(classId, schoolId)
    suspend fun getFaceWithDetails(faceId: String, schoolId: String) = localDataSource.getFaceWithDetails(faceId, schoolId)
    suspend fun getClassIdsForFace(faceId: String, schoolId: String) = localDataSource.getClassIdsForFace(faceId, schoolId)
    suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String) = localDataSource.deleteAssignmentsByFace(faceId, schoolId)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity) = localDataSource.insertAssignment(assignment)
    suspend fun upsertFace(face: FaceEntity) = localDataSource.upsertFace(face)
    
    // Remote delegation
    suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity) = remoteDataSource.syncFaceAssignment(assignment)
    suspend fun bulkSyncFaces(schoolId: String, faces: List<FaceEntity>) = remoteDataSource.bulkSyncFaces(schoolId, faces)
    suspend fun uploadFacePhoto(schoolId: String, faceId: String, imageBytes: ByteArray) = remoteDataSource.uploadFacePhoto(schoolId, faceId, imageBytes)
}
