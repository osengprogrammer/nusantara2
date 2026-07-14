package com.azuratech.azuratime.features.dashboard.ui
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails

data class DashboardUiState(
    val isLoading: Boolean = false,
    val account: AccountEntity? = null,
    val currentSchool: School? = null,
    val activeSession: SessionWithDetails? = null, // 🔥 v3.4.0 Smart Session
    val allSessionsToday: List<SessionWithDetails> = emptyList(), // 🔥 Fallback Picker
    val assignedClasses: List<ClassModel> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(),
    val recentRecords: List<AttendanceRecord> = emptyList(),
    val sessionStudents: List<StudentBiometricEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val isReady: Boolean = false,
    val pendingRequests: Int = 0,
    val currentRole: String = "GUEST",
    val isApproved: Boolean = false,
    val showSupervisorOnboarding: Boolean = false,
    val totalStudents: Int = 0,
    val totalActiveStudents: Int = 0,
    val unassignedStudents: Int = 0,
    val brokenAssignments: Int = 0,
    val unsyncedRecords: Int = 0,
    val conflicts: List<AttendanceConflict> = emptyList(),
    val geofence: com.azuratech.azuratime.features.school.data.local.GpsGeofenceEntity? = null,
    val error: String? = null,
    val isLoggingOut: Boolean = false,
)
