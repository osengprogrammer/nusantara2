package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val schoolId: String,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val metricsJson: String,
    val isSynced: Boolean = false
)
