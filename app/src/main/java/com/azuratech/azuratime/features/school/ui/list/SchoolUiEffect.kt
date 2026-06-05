package com.azuratech.azuratime.features.school.ui.list

/**
 * 🚀 SCHOOL UI EFFECT (v3.2.0-ai-native)
 * Transient events for school list and workspace switching.
 */
sealed class SchoolUiEffect {
    data class ShowSnackbar(val message: String) : SchoolUiEffect()
    data class NavigateTo(val route: String) : SchoolUiEffect()
}
