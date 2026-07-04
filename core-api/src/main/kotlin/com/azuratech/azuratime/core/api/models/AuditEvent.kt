package com.azuratech.azuratime.core.api.models

data class AuditEvent(
    val timestamp: Long,
    val action: String,
    val itemId: String,
    val status: String,
)
