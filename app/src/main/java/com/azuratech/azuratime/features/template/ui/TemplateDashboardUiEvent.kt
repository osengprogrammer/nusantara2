package com.azuratech.azuratime.features.template.ui

import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate

/**
 * 📢 TemplateDashboardUiEvent.kt (v1.0.0-ai-native)
 */
sealed class TemplateDashboardUiEvent {
    data object LoadTemplates : TemplateDashboardUiEvent()
    data class ApplyTemplate(val template: SchoolTemplate) : TemplateDashboardUiEvent()
}
