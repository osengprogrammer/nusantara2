package com.azuratech.azuratime.features.school.ui.classes

/**
 * 🚀 CLASS UI EFFECT (v3.2.0-ai-native)
 * Transient events for class management.
 */
sealed class ClassUiEffect {
    data class ShowSnackbar(val message: String) : ClassUiEffect()
    data class NavigateTo(val route: String) : ClassUiEffect()
}
