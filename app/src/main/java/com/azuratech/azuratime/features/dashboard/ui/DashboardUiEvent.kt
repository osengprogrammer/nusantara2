package com.azuratech.azuratime.features.dashboard.ui

import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict

sealed class DashboardUiEvent {
    data object LoadDashboard : DashboardUiEvent()
    data object Refresh : DashboardUiEvent()
    data class SelectSchool(val school: School) : DashboardUiEvent()
    data class SelectActiveClass(val classId: String?) : DashboardUiEvent()
    data class ResolveConflict(val conflict: AttendanceConflict, val useCloud: Boolean) : DashboardUiEvent()
    data class NavigateTo(val route: String) : DashboardUiEvent()
    data object Logout : DashboardUiEvent()
    data object OnRegisterStudentClick : DashboardUiEvent()
}
