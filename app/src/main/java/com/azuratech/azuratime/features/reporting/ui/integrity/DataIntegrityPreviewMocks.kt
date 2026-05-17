package com.azuratech.azuratime.features.reporting.ui.integrity

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus

/**
 * 🏰 DATA INTEGRITY PREVIEW MOCKS
 */
object DataIntegrityPreviewMocks {
    fun success(): DataIntegrityUiState = DataIntegrityUiState(
        totalStudents = 120,
        totalRecords = 4500,
        missingAssignments = 0,
        brokenAssignments = 0,
        unsyncedCount = 0,
        conflicts = emptyList(),
    )

    fun conflicted(): DataIntegrityUiState = DataIntegrityUiState(
        totalStudents = 120,
        totalRecords = 4500,
        missingAssignments = 5,
        brokenAssignments = 2,
        unsyncedCount = 12,
        conflicts = listOf(
            AttendanceConflict(
                conflictId = "conf_1",
                local = AttendanceRecord(
                    recordId = "rec_1",
                    studentId = "stu_1",
                    studentName = "Budi Santoso",
                    classId = "cls_1",
                    className = "10A",
                    schoolId = "sch_1",
                    timestamp = System.currentTimeMillis(),
                    status = AttendanceStatus.PRESENT,
                ),
                cloud = AttendanceRecord(
                    recordId = "rec_1",
                    studentId = "stu_1",
                    studentName = "Budi Santoso",
                    classId = "cls_1",
                    className = "10A",
                    schoolId = "sch_1",
                    timestamp = System.currentTimeMillis(),
                    status = AttendanceStatus.ABSENT,
                ),
            ),
        ),
    )

    fun loading(): DataIntegrityUiState = DataIntegrityUiState(isLoading = true)
}
