package com.azuratech.azuratime.core.api.audit

import com.azuratech.azuratime.core.api.models.AuditEvent

interface AuditRepository {
    suspend fun logEvent(event: AuditEvent)
    suspend fun getRecentEvents(): List<AuditEvent>
}
