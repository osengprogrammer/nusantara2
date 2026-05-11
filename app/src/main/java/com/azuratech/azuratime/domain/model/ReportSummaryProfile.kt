package com.azuratech.azuratime.domain.model

/**
 * 📊 REPORT SUMMARY PROFILE - UI model for high-level school analytics
 */
data class SchoolAnalyticsSummary(
    val reportId: String,
    val reportName: String,
    val dateRange: String,
    val metrics: Map<String, String>,
    val syncStatus: SyncStatus
)
