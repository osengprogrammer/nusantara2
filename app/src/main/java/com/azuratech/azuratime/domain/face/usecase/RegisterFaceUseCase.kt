package com.azuratech.azuratime.domain.face.usecase

import android.app.Application
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuraengine.face.RegisterResult
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ml.matcher.FaceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RegisterFaceUseCase @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager,
    private val syncFaces: SyncFacesUseCase,
    private val photoStorageUtils: PhotoStorageUtils
) {
    suspend operator fun invoke(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBytes: ByteArray?
    ): Result<RegisterResult> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            
            // Sync before registration
            syncFaces()

            val allEnrolled = localDataSource.getAllFacesForScanningList(schoolId)
            val currentGallery = allEnrolled.map { it.name to (it.embedding ?: floatArrayOf()) }

            if (currentGallery.isNotEmpty()) {
                val matchResult = FaceEngine.findBestMatch(
                    inputEmbedding = embedding,
                    gallery = currentGallery,
                    isRegistrationMode = true
                )
                if (matchResult is FaceEngine.MatchResult.DuplicateFound) {
                    return@withContext Result.Success(RegisterResult.Duplicate(matchResult.existingName))
                }
            }

            val finalFaceId = if (inputId.contains("--")) inputId else "${classId}--${inputId}"
            val existingFace = localDataSource.getFaceById(finalFaceId, schoolId)
            if (existingFace != null) return@withContext Result.Success(RegisterResult.Duplicate(existingFace.name))

            var finalPhotoUrl: String? = photoBytes?.let {
                photoStorageUtils.saveFacePhoto(it, finalFaceId)
            }

            photoBytes?.let { bytes ->
                val uploadResult = remoteDataSource.uploadFacePhoto(schoolId, finalFaceId, bytes)
                if (uploadResult is Result.Success) {
                    finalPhotoUrl = uploadResult.data
                }
            }

            val face = FaceEntity(
                faceId = finalFaceId,
                name = name,
                photoUrl = finalPhotoUrl,
                embedding = embedding,
                schoolId = schoolId,
                isSynced = false
            )
            localDataSource.upsertFace(face)

            val assignment = FaceAssignmentEntity(
                faceId = finalFaceId,
                classId = classId,
                schoolId = schoolId,
                isSynced = false
            )
            localDataSource.insertAssignment(assignment)

            try {
                val syncRes1 = remoteDataSource.bulkSyncFaces(schoolId, listOf(face))
                if (syncRes1 is Result.Failure) throw Exception("Sync failed")
                
                val syncRes2 = remoteDataSource.syncFaceAssignment(assignment)
                if (syncRes2 is Result.Failure) throw Exception("Assignment sync failed")
                
                localDataSource.upsertFace(face.copy(isSynced = true))
            } catch (e: Exception) {
                println("ERROR: [RegisterFaceUseCase] Gagal sync cloud: ${e.message}")
            }

            FaceCache.refresh(application, schoolId)
            Result.Success(RegisterResult.Success)

        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
