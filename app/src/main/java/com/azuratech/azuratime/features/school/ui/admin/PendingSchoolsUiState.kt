package com.azuratech.azuratime.features.school.ui.admin

import com.azuratech.azuraengine.model.School

/**
 * 👑 PENDING SCHOOLS UI STATE (v3.2.0-ai-native)
 */
data class PendingSchoolsUiState(
    val pendingSchools: List<School> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSchoolId: String? = null,
)
