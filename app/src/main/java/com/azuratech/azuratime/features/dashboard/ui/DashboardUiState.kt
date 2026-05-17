package com.azuratech.azuratime.features.dashboard.ui

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity

data class DashboardUiState(
    val account: AccountEntity? = null,
    val assignedClasses: List<ClassModel> = emptyList(),
    val allClasses: List<ClassModel> = emptyList(), 
    val recentRecords: List<AttendanceRecordEntity> = emptyList(), // SSOT: using Entity
    val sessionStudents: List<StudentBiometricEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val isReady: Boolean = false,
    val pendingRequests: Int = 0,
    val currentRole: String = "ACCOUNT",
    val isApproved: Boolean = false,
    // Integrity Data
    val totalStudents: Int = 0,
    val unassignedStudents: Int = 0,
    val brokenAssignments: Int = 0,
    val unsyncedRecords: Int = 0,
    val conflicts: List<AttendanceConflict> = emptyList()
)
