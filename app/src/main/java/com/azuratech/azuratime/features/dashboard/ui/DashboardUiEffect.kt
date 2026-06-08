package com.azuratech.azuratime.features.dashboard.ui

/**
 * 🏠 DASHBOARD UI EFFECT (v3.2.2-ai-native)
 */
sealed class DashboardUiEffect {
    data class ShowToast(val message: String) : DashboardUiEffect()
    data class ShowSnackbar(val message: String) : DashboardUiEffect()
    data class NavigateTo(val route: String) : DashboardUiEffect()
    data object TriggerAtomicExit : DashboardUiEffect()
}
