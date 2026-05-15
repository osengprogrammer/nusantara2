package com.azuratech.azuratime.features.reporting.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 📤 EXPORT JOB PROFILE - UI model for background export tasks
 */
data class ExportJobProfile(
    val jobId: String,
    val fileType: String,
    val status: String,
    val filePath: String?,
    val syncStatus: SyncStatus
)
