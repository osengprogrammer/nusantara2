package com.azuratech.azuratime.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class RegisterResult {
    object Success : RegisterResult()
    data class Duplicate(val name: String) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

@Singleton
class FaceRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    @Deprecated("Route through UseCase layer")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allFacesWithDetailsFlow: Flow<Result<List<FaceWithDetails>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                kotlinx.coroutines.flow.combine(
                    localDataSource.getAllFacesFlow(schoolId),
                    localDataSource.observeClassesBySchool(schoolId),
                    localDataSource.getAllAssignmentsFlow(schoolId)
                ) { faces, classes, assignments ->
                    val classMap = classes.associateBy { it.id }
                    val assignmentMap = assignments.groupBy { it.faceId }
                    
                    val detailedFaces = faces.map { face ->
                        val userAssignments = assignmentMap[face.faceId] ?: emptyList()
                        val classNames = userAssignments.mapNotNull { classMap[it.classId]?.name }.joinToString(", ")
                        
                        FaceWithDetails(
                            face = face,
                            className = classNames.ifEmpty { null },
                            classId = userAssignments.firstOrNull()?.classId
                        )
                    }.sortedBy { it.face.name }
                    
                    Result.Success(detailedFaces) as Result<List<FaceWithDetails>>
                }
            }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    @Deprecated("Route through UseCase layer")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val facesForScanningFlow: Flow<Result<List<FaceEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getAllFacesForScanningFlow(schoolId)
            }
            .map { Result.Success(it) as Result<List<FaceEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    @Deprecated("Route through UseCase layer")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFacesInClassFlow(classId: String): Flow<Result<List<FaceEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getFacesInClassFlow(classId, schoolId)
            }
            .map { Result.Success(it) as Result<List<FaceEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    @Deprecated("Route through UseCase layer")
    suspend fun getFaceWithDetails(faceId: String): Result<FaceWithDetails?> = withContext(Dispatchers.IO) {
        try {
            Result.Success(localDataSource.getFaceWithDetails(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun getAssignmentsForFace(faceId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.Success(localDataSource.getClassIdsForFace(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun performFaceDeltaSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val lastSync = sessionManager.getLastFacesSyncTime()
            val syncResult = remoteDataSource.getFaceUpdates(schoolId, lastSync)
            
            if (syncResult is Result.Success) {
                val updatedData = syncResult.data
                if (updatedData.isNotEmpty()) {
                    val toUpsert = updatedData.filter { pair -> pair.second }.map { pair -> pair.first }
                    val toDelete = updatedData.filter { pair -> !pair.second }.map { pair -> pair.first }

                    if (toUpsert.isNotEmpty()) {
                        localDataSource.upsertAll(toUpsert)
                    }

                    if (toDelete.isNotEmpty()) {
                        for (face in toDelete) {
                            localDataSource.deleteFaceById(face.faceId, schoolId)
                        }
                    }

                    FaceCache.refresh(application, schoolId)
                    sessionManager.saveLastFacesSyncTime(System.currentTimeMillis())
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun registerFace(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBitmap: Bitmap?
    ): Result<RegisterResult> = withContext(Dispatchers.IO) {
        try {
            performFaceDeltaSync()

            val allEnrolled = localDataSource.getAllFacesForScanningList(schoolId)
            val currentGallery = allEnrolled.map { it.name to (it.embedding ?: floatArrayOf()) }

            if (currentGallery.isNotEmpty()) {
                val matchResult = com.azuratech.azuratime.ml.matcher.FaceEngine.findBestMatch(
                    inputEmbedding = embedding,
                    gallery = currentGallery,
                    isRegistrationMode = true
                )
                if (matchResult is com.azuratech.azuratime.ml.matcher.FaceEngine.MatchResult.DuplicateFound) {
                    return@withContext Result.Success(RegisterResult.Duplicate(matchResult.existingName))
                }
            }

            val finalFaceId = if (inputId.contains("--")) inputId else "${classId}--${inputId}"
            val existingFace = localDataSource.getFaceById(finalFaceId, schoolId)
            if (existingFace != null) return@withContext Result.Success(RegisterResult.Duplicate(existingFace.name))

            var finalPhotoUrl: String? = photoBitmap?.let {
                PhotoStorageUtils.saveFacePhoto(application, it, finalFaceId)
            }

            photoBitmap?.let { bmp ->
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val uploadResult = remoteDataSource.uploadFacePhoto(schoolId, finalFaceId, stream.toByteArray())
                if (uploadResult is Result.Success) {
                    finalPhotoUrl = uploadResult.data
                }
            }

            val face = FaceEntity(faceId = finalFaceId, name = name, photoUrl = finalPhotoUrl, embedding = embedding, schoolId = schoolId, isSynced = false)
            localDataSource.upsertFace(face)

            val assignment = FaceAssignmentEntity(faceId = finalFaceId, classId = classId, schoolId = schoolId, isSynced = false)
            localDataSource.insertAssignment(assignment)

            try {
                val syncRes1 = remoteDataSource.bulkSyncFaces(schoolId, listOf(face))
                if (syncRes1 is Result.Failure) throw Exception("Sync failed")
                
                val syncRes2 = remoteDataSource.syncFaceAssignment(assignment)
                if (syncRes2 is Result.Failure) throw Exception("Assignment sync failed")
                
                localDataSource.upsertFace(face.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("FaceRepository", "Gagal sync cloud: ${e.message}")
            }

            FaceCache.refresh(application, schoolId)
            Result.Success(RegisterResult.Success)

        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun deleteFace(face: FaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val targetSchoolId = face.schoolId.ifEmpty { schoolId }
        try {
            face.photoUrl?.let { PhotoStorageUtils.deleteFacePhoto(it) }

            val classIds = localDataSource.getClassIdsForFace(face.faceId, targetSchoolId)
            val remoteDeleteRes = remoteDataSource.deleteFace(face.faceId, targetSchoolId, classIds)
            
            if (remoteDeleteRes is Result.Failure) {
                Log.e("FaceRepository", "Gagal hapus di Cloud: ${remoteDeleteRes.error}")
            }

            localDataSource.deleteFace(face)
            localDataSource.deleteAssignmentsByFace(face.faceId, targetSchoolId)

            FaceCache.refresh(application, targetSchoolId)
            Result.Success(Unit)
            
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun updateEmployeeClass(faceId: String, newClassId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localDataSource.deleteAssignmentsByFace(faceId, schoolId)
            if (newClassId != null) {
                val assignment = FaceAssignmentEntity(faceId = faceId, classId = newClassId, schoolId = schoolId, isSynced = false)
                localDataSource.insertAssignment(assignment)
                val syncRes = remoteDataSource.syncFaceAssignment(assignment)
                if (syncRes is Result.Failure) throw Exception("Sync failed")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun updateFaceBasic(face: FaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedFace = face.copy(lastUpdated = System.currentTimeMillis(), isSynced = false)
            localDataSource.upsertFace(updatedFace)
            
            val syncRes = remoteDataSource.bulkSyncFaces(schoolId, listOf(updatedFace))
            if (syncRes is Result.Failure) throw Exception("Sync failed")
            
            localDataSource.upsertFace(updatedFace.copy(isSynced = true))
            FaceCache.refresh(application, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun updateFaceWithPhoto(face: FaceEntity, photoBitmap: Bitmap?, embedding: FloatArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var finalPhotoUrl = photoBitmap?.let { PhotoStorageUtils.saveFacePhoto(application, it, face.faceId) } ?: face.photoUrl
            
            if (photoBitmap != null) {
                val stream = java.io.ByteArrayOutputStream()
                photoBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val uploadResult = remoteDataSource.uploadFacePhoto(schoolId, face.faceId, stream.toByteArray())
                if (uploadResult is Result.Success) {
                    finalPhotoUrl = uploadResult.data
                }
            }

            val updatedFace = face.copy(photoUrl = finalPhotoUrl, embedding = embedding, lastUpdated = System.currentTimeMillis(), isSynced = false)
            localDataSource.upsertFace(updatedFace)
            
            val syncRes = remoteDataSource.bulkSyncFaces(schoolId, listOf(updatedFace))
            if (syncRes is Result.Failure) throw Exception("Sync failed")
            
            localDataSource.upsertFace(updatedFace.copy(isSynced = true))
            FaceCache.refresh(application, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun uploadFacePhotoToCloud(schoolId: String, faceId: String, imageBytes: ByteArray): Result<String?> =
        remoteDataSource.uploadFacePhoto(schoolId, faceId, imageBytes)

    @Deprecated("Route through UseCase layer")
    suspend fun bulkSyncFacesToCloud(schoolId: String, faces: List<FaceEntity>): Result<Unit> =
        remoteDataSource.bulkSyncFaces(schoolId, faces)

    @Deprecated("Route through UseCase layer")
    suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity): Result<Unit> =
        remoteDataSource.syncFaceAssignment(assignment)
}
