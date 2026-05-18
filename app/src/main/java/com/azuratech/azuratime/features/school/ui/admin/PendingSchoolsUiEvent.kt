package com.azuratech.azuratime.features.school.ui.admin

/**
 * 👑 PENDING SCHOOLS UI EVENT (v3.2.0-ai-native)
 */
sealed class PendingSchoolsUiEvent {
    object LoadPending : PendingSchoolsUiEvent()
    data class ApproveSchool(val schoolId: String) : PendingSchoolsUiEvent()
    data class RejectSchool(val schoolId: String, val reason: String) : PendingSchoolsUiEvent()
    object ClearError : PendingSchoolsUiEvent()
}
