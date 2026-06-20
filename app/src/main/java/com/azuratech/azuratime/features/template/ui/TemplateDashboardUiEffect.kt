package com.azuratech.azuratime.features.template.ui

/**
 * ⚡ TemplateDashboardUiEffect.kt (v1.0.0-ai-native)
 */
sealed class TemplateDashboardUiEffect {
    data class ShowToast(val message: String) : TemplateDashboardUiEffect()
    data class ShowSnackbar(val message: String) : TemplateDashboardUiEffect()
    data object NavigateBack : TemplateDashboardUiEffect()
}
