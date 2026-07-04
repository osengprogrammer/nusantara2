package com.azuratech.azuratime.feature.audit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val action: String,
    val itemId: String? = null, // Nullable as not all events might have an itemId
    val status: String
)
