package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.AuditLogEntity
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val auditLogDao = database.auditLogDao()

    fun observeLogsBySchool(schoolId: String): Flow<List<AuditLogEntity>> =
        auditLogDao.observeLogsBySchool(schoolId)

    suspend fun logAction(schoolId: String, userId: String, action: String, details: String? = null): Result<Unit> {
        return try {
            val log = AuditLogEntity(
                logId = "LOG_${System.currentTimeMillis()}",
                schoolId = schoolId,
                userId = userId,
                action = action,
                timestamp = System.currentTimeMillis(),
                details = details
            )
            auditLogDao.insertLog(log)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.LocalDB(e.message))
        }
    }
}
