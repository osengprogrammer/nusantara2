package com.azuratech.azuratime.data.repo

import android.app.Application
import android.graphics.Bitmap
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricFaceRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager,
    private val studentRepository: com.azuratech.azuratime.domain.student.repository.StudentRepository
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    // Delegation methods for DAOs / DataSources
    fun observeEnrollmentsBySchool(schoolId: String) = localDataSource.getAllFacesFlow(schoolId)

    suspend fun submitEnrollment(studentId: String, photoUri: String): Result<Unit> {
        return try {
            val face = localDataSource.getFaceByStudentId(studentId, schoolId)
            if (face != null) {
                localDataSource.upsertFace(face.copy(photoUrl = photoUri, isSynced = false))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    suspend fun deleteEnrollment(faceId: String): Result<Unit> {
        return try {
            localDataSource.markPendingDeletion(faceId, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }

    fun getAllFacesFlow(schoolId: String) = localDataSource.getAllFacesFlow(schoolId)
    fun getFacesWithDetailsFlow(schoolId: String) = localDataSource.getAllFacesWithDetailsFlow(schoolId)
    fun getEnrolledFacesFlow(schoolId: String) = localDataSource.getAllFacesForScanningFlow(schoolId)
    fun getAllFacesForScanningFlow(schoolId: String) = localDataSource.getAllFacesForScanningFlow(schoolId)
    fun getFacesInClassFlow(classId: String, schoolId: String) = localDataSource.getFacesInClassFlow(classId, schoolId)
    suspend fun getFaceWithDetails(faceId: String, schoolId: String) = localDataSource.getFaceWithDetails(faceId, schoolId)
    suspend fun getClassIdsForFace(faceId: String, schoolId: String) = localDataSource.getClassIdsForFace(faceId, schoolId)
    suspend fun deleteAssignmentsByFace(faceId: String, schoolId: String) = localDataSource.deleteAssignmentsByFace(faceId, schoolId)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity) = localDataSource.insertAssignment(assignment)
    suspend fun upsertFace(face: BiometricFaceEntity) = localDataSource.upsertFace(face)

    suspend fun assignStudentToClass(faceId: String, classId: String): Result<Unit> = try {
        localDataSource.insertAssignment(FaceAssignmentEntity(faceId, classId, schoolId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun removeStudentFromClass(faceId: String, classId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByFace(faceId, schoolId) // Simple version: remove all then re-add if needed, or specific delete
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun removeAllAssignmentsForFace(faceId: String): Result<Unit> = try {
        localDataSource.deleteAssignmentsByFace(faceId, schoolId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun deleteFace(faceId: String) = deleteEnrollment(faceId)

    suspend fun updateFaceClass(faceId: String, classId: String?): Result<Unit> = try {
        localDataSource.deleteAssignmentsByFace(faceId, schoolId)
        if (classId != null) {
            localDataSource.insertAssignment(FaceAssignmentEntity(faceId, classId, schoolId))
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
    }

    suspend fun saveStudentProfile(profile: com.azuratech.azuratime.domain.model.StudentProfile, photoBytes: ByteArray? = null): Result<Unit> {
        // Simple delegation to StudentRepository
        return studentRepository.saveProfile(profile)
    }
    
    /**
     * 🔥 SSOT: Pull face updates from cloud to local Room.
     */
    suspend fun syncFaces(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastSync = sessionManager.getLastFacesSyncTime()
            val syncResult = remoteDataSource.getFaceUpdates(schoolId, lastSync)
            
            if (syncResult is Result.Success) {
                val updatedData = syncResult.data
                if (updatedData.isNotEmpty()) {
                    val toUpsert = updatedData.filter { it.second }.map { it.first }
                    val toDelete = updatedData.filter { !it.second }.map { it.first }

                    if (toUpsert.isNotEmpty()) localDataSource.upsertAll(toUpsert)
                    if (toDelete.isNotEmpty()) {
                        toDelete.forEach { localDataSource.deleteFaceById(it.faceId, schoolId) }
                    }

                    FaceCache.refresh(application, schoolId)
                    sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    /**
     * 🔥 SSOT: Pull assignments from cloud to local Room.
     */
    suspend fun syncAssignments(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)
        try {
            // This is a simplified version of SyncAssignmentsUseCase logic
            // In a real app, you'd fetch from Firestore here. 
            // For now, we'll keep it as a placeholder that delegatest to remoteDataSource if needed,
            // or we'll implement the logic here if we have Firestore access.
            // Since FaceRemoteDataSource doesn't have getAssignments, we might need to add it.
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }

    // Remote delegation
    suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity) = remoteDataSource.syncFaceAssignment(assignment)
    suspend fun bulkSyncFaces(schoolId: String, faces: List<BiometricFaceEntity>) = remoteDataSource.bulkSyncFaces(schoolId, faces)
    suspend fun uploadFacePhoto(schoolId: String, faceId: String, imageBytes: ByteArray) = remoteDataSource.uploadFacePhoto(schoolId, faceId, imageBytes)
}
