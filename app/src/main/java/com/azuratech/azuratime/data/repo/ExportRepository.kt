package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ExportJobEntity
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val exportJobDao = database.exportJobDao()

    fun observeExportJobsByUser(userId: String): Flow<List<ExportJobEntity>> =
        exportJobDao.observeExportJobsByUser(userId)

    suspend fun createExportJob(fileType: String): Result<Unit> {
        return try {
            val userId = "MOCK_USER" // Should be fetched from session
            val job = ExportJobEntity(
                jobId = "JOB_${System.currentTimeMillis()}",
                userId = userId,
                fileType = fileType,
                status = "PENDING"
            )
            exportJobDao.insertJob(job)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }
}
