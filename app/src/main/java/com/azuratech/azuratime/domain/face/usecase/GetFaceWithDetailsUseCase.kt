package com.azuratech.azuratime.domain.face.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to get details of a specific face.
 */
class GetFaceWithDetailsUseCase @Inject constructor(
    private val localDataSource: FaceLocalDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(faceId: String): Result<FaceWithDetails?> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            Result.Success(localDataSource.getFaceWithDetails(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
