package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.reporting.data.local.ExportJobEntity
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val database: AppDatabase,
) {
    private val exportJobDao = database.exportJobDao()

    fun observeExportJobs(userId: String): Flow<List<ExportJobProfile>> {
        return exportJobDao.observeExportJobsByUser(userId).map { entities ->
            entities.map { entity ->
                ExportJobProfile(
                    jobId = entity.jobId,
                    fileType = entity.fileType,
                    status = entity.status,
                    filePath = entity.filePath,
                    syncStatus = if (entity.isSynced) {
                        com.azuratech.azuratime.core.domain.model.SyncStatus.SYNCED
                    } else {
                        com.azuratech.azuratime.core.domain.model.SyncStatus.PENDING_INSERT
                    },
                )
            }
        }
    }

    suspend fun startExport(format: String, userId: String, schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jobId = UUID.randomUUID().toString()
            val job = ExportJobEntity(
                jobId = jobId,
                userId = userId,
                fileType = format,
                status = "PENDING",
                filePath = null,
                isSynced = false,
            )
            exportJobDao.insertJob(job)
            Result.Success(jobId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun clearCompletedJobs(userId: String) {
        exportJobDao.clearCompletedJobs(userId)
    }
}
