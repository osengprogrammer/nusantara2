package com.azuratech.azuratime.features.attendance.ui

/**
 * ⚡ ATTENDANCE UI EFFECT (v3.2.0-ai-native)
 * Represents one-time side effects like Navigation, Toasts, or Snackbars.
 * Decoupled from persistent UiState to prevent duplicate triggers.
 */
sealed class AttendanceUiEffect {
    data class ShowToast(val message: String) : AttendanceUiEffect()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : AttendanceUiEffect()
    data class NavigateToDetail(val recordId: String) : AttendanceUiEffect()
    data object NavigateBack : AttendanceUiEffect()
    data class ExportSuccess(val path: String) : AttendanceUiEffect()
}
