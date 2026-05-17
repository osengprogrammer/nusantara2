package com.azuratech.azuratime.features.reporting.ui

import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import java.time.LocalDate

/**
 * 📊 REPORT UI STATE
 * v3.2.0-ai-native compliant
 */
data class ReportUiState(
    val isLoading: Boolean = false,
    val startDate: LocalDate = LocalDate.now().minusDays(30),
    val endDate: LocalDate = LocalDate.now(),
    val auditLogs: List<SystemAuditTrail> = emptyList(),
    val exportJobs: List<ExportJobProfile> = emptyList(),
    val error: String? = null,
    val selectedTab: ReportTab = ReportTab.AuditLogs,
)

enum class ReportTab {
    AuditLogs, ExportJobs
}
