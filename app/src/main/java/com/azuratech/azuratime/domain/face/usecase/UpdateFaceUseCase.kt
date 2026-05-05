package com.azuratech.azuratime.domain.face.usecase

import android.app.Application
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update basic face information (name, metadata) and trigger sync.
 */
@Deprecated(
    message = "Use SaveStudentProfileUseCase. Migration: Replace with StudentProfile + SaveStudentProfileUseCase",
    replaceWith = ReplaceWith("SaveStudentProfileUseCase")
)
class UpdateFaceUseCase @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(face: FaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val updatedFace = face.copy(
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
            localDataSource.upsertFace(updatedFace)

            // Trigger remote sync
            val syncRes = remoteDataSource.bulkSyncFaces(schoolId, listOf(updatedFace))
            
            if (syncRes is Result.Success) {
                localDataSource.upsertFace(updatedFace.copy(isSynced = true))
            }

            FaceCache.refresh(application, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
