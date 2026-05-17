package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.reporting.data.local.AuditLogEntity
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepository @Inject constructor(
    private val database: AppDatabase,
) {
    private val auditLogDao = database.auditLogDao()

    fun observeAuditLogs(schoolId: String): Flow<List<SystemAuditTrail>> {
        return auditLogDao.observeLogsBySchool(schoolId)
            .map { entities -> entities.map { it.toProfile() } }
    }

    suspend fun logAction(schoolId: String, accountId: String, action: String, details: String? = null) {
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
    }

    suspend fun purgeLogs(schoolId: String, beforeTimestamp: Long) {
        auditLogDao.purgeOldLogs(schoolId, beforeTimestamp)
    }
}
