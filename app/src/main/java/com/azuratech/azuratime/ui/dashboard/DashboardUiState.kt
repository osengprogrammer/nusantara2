package com.azuratech.azuratime.ui.dashboard

import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.local.AttendanceConflict

data class DashboardUiState(
    val user: UserEntity? = null,
    val assignedClasses: List<ClassModel> = emptyList(),
    val recentRecords: List<CheckInRecordEntity> = emptyList(),
    val sessionStudents: List<FaceEntity> = emptyList(), // Changed to FaceEntity
    val isSyncing: Boolean = false,
    val pendingRequests: Int = 0,
    val currentRole: String = "USER",
    val isApproved: Boolean = false,
    // Integrity Data
    val totalFaces: Int = 0,
    val unassignedStudents: Int = 0,
    val brokenAssignments: Int = 0,
    val unsyncedRecords: Int = 0,
    val conflicts: List<AttendanceConflict> = emptyList()
)
