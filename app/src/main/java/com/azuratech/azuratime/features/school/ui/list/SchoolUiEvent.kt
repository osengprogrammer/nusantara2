package com.azuratech.azuratime.features.school.ui.list

import com.azuratech.azuraengine.model.School

sealed class SchoolUiEvent {
    data class LoadSchools(val accountId: String) : SchoolUiEvent()
    data class SelectSchool(val school: School) : SchoolUiEvent()
    data class CreateSchool(val name: String, val timezone: String, val selectedClassIds: List<String>) : SchoolUiEvent()
    data class DeleteSchool(val id: String) : SchoolUiEvent()
    data object Retry : SchoolUiEvent()
}
