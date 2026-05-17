package com.azuratech.azuratime.features.school.ui.list

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School

data class SchoolUiState(
    val isLoading: Boolean = false,
    val schools: List<School> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val activeSchoolId: String? = null,
    val error: String? = null,
    val accountId: String = "",
)
