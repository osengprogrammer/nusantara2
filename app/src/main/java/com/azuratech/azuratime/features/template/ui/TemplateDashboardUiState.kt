package com.azuratech.azuratime.features.template.ui

import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate

/**
 * 📄 TemplateDashboardUiState.kt (v1.1.0-ai-native)
 */
data class EnrichedSchoolTemplate(
    val template: SchoolTemplate,
    val classNames: List<String> = emptyList(),
    val subjectNames: List<String> = emptyList(),
)

data class TemplateDashboardUiState(
    val isLoading: Boolean = false,
    val templates: List<EnrichedSchoolTemplate> = emptyList(),
    val isApplying: Boolean = false,
    val error: String? = null,
)
