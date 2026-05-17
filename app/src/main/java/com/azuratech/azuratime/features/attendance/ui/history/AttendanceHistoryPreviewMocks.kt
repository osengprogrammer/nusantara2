package com.azuratech.azuratime.features.attendance.ui.history

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus

object AttendanceHistoryPreviewMocks {
    fun loading(): AttendanceHistoryUiState = AttendanceHistoryUiState(isLoading = true)

    fun success(): AttendanceHistoryUiState = AttendanceHistoryUiState(
        records = List(5) { i ->
            AttendanceRecord(
                recordId = "rec_$i",
                studentId = "stu_preview",
                studentName = "Student Preview $i",
                classId = "cls_1",
                className = "Kelas 10A",
                schoolId = "sch_1",
                timestamp = System.currentTimeMillis() - (i * 3600000),
                status = AttendanceStatus.PRESENT,
                isSynced = true,
                accountEmail = "admin@azuratech.com",
            )
        },
        studentId = "stu_preview",
    )

    fun error(): AttendanceHistoryUiState = AttendanceHistoryUiState(error = "Failed to load history")
}
