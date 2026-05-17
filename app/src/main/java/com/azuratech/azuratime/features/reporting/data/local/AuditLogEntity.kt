package com.azuratech.azuratime.features.reporting.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log_table")
data class AuditLogEntity(
    @PrimaryKey val logId: String,
    val schoolId: String,
    val userId: String,
    val action: String,
    val timestamp: Long,
    val details: String? = null,
    val isSynced: Boolean = false,
)
