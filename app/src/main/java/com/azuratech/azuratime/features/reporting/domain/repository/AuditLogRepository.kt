package com.azuratech.azuratime.features.reporting.domain.repository

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    fun observeAuditLogs(schoolId: String): Flow<Result<List<SystemAuditTrail>>>
    suspend fun logAction(schoolId: String, accountId: String, action: String, details: String? = null): Result<Unit>
    suspend fun purgeLogs(schoolId: String, beforeTimestamp: Long): Result<Unit>
}
