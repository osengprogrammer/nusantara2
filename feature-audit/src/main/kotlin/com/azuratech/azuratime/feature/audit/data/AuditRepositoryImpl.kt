package com.azuratech.azuratime.feature.audit.data

import com.azuratech.azuratime.core.api.audit.AuditRepository
import com.azuratech.azuratime.core.api.models.AuditEvent
import com.azuratech.azuratime.feature.audit.data.local.AuditDao
import com.azuratech.azuratime.feature.audit.data.local.AuditEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditDao: AuditDao
) : AuditRepository {

    override suspend fun logEvent(event: AuditEvent) {
        val auditEntity = AuditEntity(
            timestamp = event.timestamp,
            action = event.action,
            itemId = event.itemId,
            status = event.status
        )
        auditDao.insertEvent(auditEntity)
    }

    override suspend fun getRecentEvents(): List<AuditEvent> {
        return auditDao.getRecentEvents().map { audit ->
            AuditEvent(
                timestamp = audit.timestamp,
                action = audit.action,
                itemId = audit.itemId ?: "",
                status = audit.status
            )
        }
    }
}
