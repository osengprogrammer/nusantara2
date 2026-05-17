package com.azuratech.azuratime.features.reporting.ui

import java.time.LocalDate

/**
 * 📊 REPORT UI EVENT
 * v3.2.0-ai-native compliant
 */
sealed class ReportUiEvent {
    data class SetDateRange(val start: LocalDate, val end: LocalDate) : ReportUiEvent()
    object RefreshData : ReportUiEvent()
    data class StartExport(val format: String) : ReportUiEvent()
    object ClearError : ReportUiEvent()
    data class SelectTab(val tab: ReportTab) : ReportUiEvent()
    object NavigateToDetail : ReportUiEvent() // Example detail navigation
    object ClearExportJobs : ReportUiEvent()
}
