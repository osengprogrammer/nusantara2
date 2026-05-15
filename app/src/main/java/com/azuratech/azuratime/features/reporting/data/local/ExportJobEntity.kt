package com.azuratech.azuratime.features.reporting.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey val jobId: String,
    val userId: String,
    val fileType: String,
    val status: String,
    val filePath: String? = null,
    val isSynced: Boolean = false
)
