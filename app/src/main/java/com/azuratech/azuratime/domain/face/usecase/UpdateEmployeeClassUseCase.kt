package com.azuratech.azuratime.domain.face.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to update the class assignment for a specific employee/face.
 */
class UpdateEmployeeClassUseCase @Inject constructor(
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(faceId: String, newClassId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            localDataSource.deleteAssignmentsByFace(faceId, schoolId)
            
            if (newClassId != null) {
                val assignment = FaceAssignmentEntity(
                    faceId = faceId,
                    classId = newClassId,
                    schoolId = schoolId,
                    isSynced = false
                )
                localDataSource.insertAssignment(assignment)
                
                // Trigger remote sync
                val syncRes = remoteDataSource.syncFaceAssignment(assignment)
                if (syncRes is Result.Failure) {
                    // We don't throw here to follow local-first, but we should log it
                    // In a production app, we might want to retry later
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
