package com.azuratech.azuratime.domain.face.usecase

import android.app.Application
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceCache
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncFacesUseCase @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
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
}
