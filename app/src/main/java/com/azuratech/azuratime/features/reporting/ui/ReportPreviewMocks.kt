package com.azuratech.azuratime.features.reporting.ui

import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 📊 REPORT PREVIEW MOCKS
 */
object ReportPreviewMocks {
    fun success(): ReportUiState = ReportUiState(
        auditLogs = listOf(
            SystemAuditTrail(
                logId = "log_1",
                userId = "user_1",
                action = "LOGIN",
                timestamp = System.currentTimeMillis(),
                details = "User logged in from Android device",
                syncStatus = SyncStatus.SYNCED,
            ),
            SystemAuditTrail(
                logId = "log_2",
                userId = "user_1",
                action = "REGISTER_STUDENT",
                timestamp = System.currentTimeMillis() - 3600000,
                details = "Student STU-123 registered",
                syncStatus = SyncStatus.SYNCED,
            ),
        ),
        exportJobs = listOf(
            ExportJobProfile(
                jobId = "job_1",
                fileType = "CSV",
                status = "COMPLETED",
                filePath = "/storage/emulated/0/Download/report.csv",
                syncStatus = SyncStatus.SYNCED,
            ),
            ExportJobProfile(
                jobId = "job_2",
                fileType = "EXCEL",
                status = "PENDING",
                filePath = null,
                syncStatus = SyncStatus.PENDING_INSERT,
            ),
        ),
    )

    fun loading(): ReportUiState = ReportUiState(isLoading = true)

    fun error(): ReportUiState = ReportUiState(error = "Gagal memuat data laporan. Periksa koneksi internet.")
}
