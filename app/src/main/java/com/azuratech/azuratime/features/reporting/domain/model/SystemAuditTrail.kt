package com.azuratech.azuratime.features.reporting.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 📝 AUDIT LOG PROFILE - UI model for system audit trails
 */
data class SystemAuditTrail(
    val logId: String,
    val accountId: String,
    val action: String,
    val timestamp: Long,
    val details: String?,
    val syncStatus: SyncStatus,
)
