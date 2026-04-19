package com.azuratech.azuratime.domain.face.usecase

import android.app.Application
import android.graphics.Bitmap
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update a face with a new photo and embedding.
 * Enforces Local-First flow by updating local storage and flagging for background sync.
 */
class UpdateFaceWithPhotoUseCase @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        face: FaceEntity,
        photoBitmap: Bitmap?,
        embedding: FloatArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            
            // 1. Validate faceId exists locally
            val existingFace = localDataSource.getFaceById(face.faceId, schoolId)
                ?: return@withContext Result.Failure(AppError.BusinessRule("Face ID not found locally"))

            // 2. If new photo provided: upload to Firebase Storage -> get URL
            var finalPhotoUrl = face.photoUrl
            if (photoBitmap != null) {
                // Save locally first for immediate use
                finalPhotoUrl = PhotoStorageUtils.saveFacePhoto(application, photoBitmap, face.faceId)
                
                // Upload to remote storage
                val stream = java.io.ByteArrayOutputStream()
                photoBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val uploadResult = remoteDataSource.uploadFacePhoto(schoolId, face.faceId, stream.toByteArray())
                
                if (uploadResult is Result.Success) {
                    finalPhotoUrl = uploadResult.data ?: finalPhotoUrl
                }
            }

            // 3. Update local FaceEntity with new metadata/photoUrl/embedding
            // 4. Set isSynced = false (queue for background sync)
            val updatedFace = face.copy(
                photoUrl = finalPhotoUrl,
                embedding = embedding,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            
            localDataSource.upsertFace(updatedFace)
            
            // Refresh cache to ensure UI reflects changes
            FaceCache.refresh(application, schoolId)
            
            // 5. Return Result.Success(Unit)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
