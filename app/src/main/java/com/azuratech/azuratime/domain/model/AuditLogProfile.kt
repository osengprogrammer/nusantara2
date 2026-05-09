package com.azuratech.azuratime.domain.model

/**
 * 📝 AUDIT LOG PROFILE - UI model for system audit trails
 */
data class AuditLogProfile(
    val logId: String,
    val userId: String,
    val action: String,
    val timestamp: Long,
    val details: String?,
    val syncStatus: SyncStatus
)
