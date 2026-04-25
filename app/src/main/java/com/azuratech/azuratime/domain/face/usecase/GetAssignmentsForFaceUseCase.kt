package com.azuratech.azuratime.domain.face.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAssignmentsForFaceUseCase @Inject constructor(
    private val localDataSource: FaceLocalDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(faceId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Failure(AppError.BusinessRule("School ID not found"))
            Result.Success(localDataSource.getClassIdsForFace(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
