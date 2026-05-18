package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.reporting.data.local.AuditLogEntity
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import com.azuratech.azuratime.features.reporting.domain.repository.AuditLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : AuditLogRepository {
    private val auditLogDao = database.auditLogDao()

    override fun observeAuditLogs(schoolId: String): Flow<Result<List<SystemAuditTrail>>> {
        return auditLogDao.observeLogsBySchool(schoolId)
            .map { entities -> Result.Success(entities.map { it.toProfile() }) as Result<List<SystemAuditTrail>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
    }

    override suspend fun logAction(schoolId: String, accountId: String, action: String, details: String?): Result<Unit> {
        return try {
            val log = AuditLogEntity(
                logId = "log_${System.currentTimeMillis()}_${accountId.take(5)}",
                schoolId = schoolId,
                accountId = accountId,
                action = action,
                timestamp = System.currentTimeMillis(),
                details = details,
                isSynced = false,
            )
            auditLogDao.insertLog(log)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun purgeLogs(schoolId: String, beforeTimestamp: Long): Result<Unit> {
        return try {
            auditLogDao.purgeOldLogs(schoolId, beforeTimestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
