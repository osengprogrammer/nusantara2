package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.reporting.data.local.ExportJobEntity
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : ExportRepository {
    private val exportJobDao = database.exportJobDao()

    override fun observeExportJobs(accountId: String): Flow<Result<List<ExportJobProfile>>> {
        return exportJobDao.observeExportJobsByAccount(accountId).map { entities ->
            val profiles = entities.map { entity ->
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
            Result.Success(profiles) as Result<List<ExportJobProfile>>
        }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
    }

    override suspend fun startExport(format: String, accountId: String, schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jobId = UUID.randomUUID().toString()
            val job = ExportJobEntity(
                jobId = jobId,
                accountId = accountId,
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

    override suspend fun clearCompletedJobs(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            exportJobDao.clearCompletedJobs(accountId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
