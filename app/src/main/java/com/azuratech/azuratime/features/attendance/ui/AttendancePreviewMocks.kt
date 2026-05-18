package com.azuratech.azuratime.features.attendance.ui

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus

/**
 * 📝 ATTENDANCE PREVIEW MOCKS (v3.2.0-ai-native)
 */
object AttendancePreviewMocks {
    fun loading(): AttendanceUiState = AttendanceUiState(isLoading = true)

    fun syncing(): AttendanceUiState = AttendanceUiState(isSyncing = true, records = success().records)

    fun empty(): AttendanceUiState = AttendanceUiState(records = emptyList())

    fun success(): AttendanceUiState = AttendanceUiState(
        records = listOf(
            AttendanceRecord(
                recordId = "1",
                studentId = "std_1",
                studentName = "Budi Santoso",
                classId = "cls_1",
                className = "Kelas 10A",
                schoolId = "sch_1",
                timestamp = System.currentTimeMillis(),
                status = AttendanceStatus.PRESENT,
                accountEmail = "admin@azuratech.com",
            ),
            AttendanceRecord(
                recordId = "2",
                studentId = "std_2",
                studentName = "Siti Aminah",
                classId = "cls_1",
                className = "Kelas 10A",
                schoolId = "sch_1",
                timestamp = System.currentTimeMillis() - 3600000,
                status = AttendanceStatus.LATE,
                accountEmail = "admin@azuratech.com",
            ),
        ),
        classes = listOf(
            ClassModel(id = "cls_1", schoolId = "sch_1", name = "Kelas 10A", grade = "10", studentCount = 20, accountId = "acc_1", createdAt = 0L),
            ClassModel(id = "cls_2", schoolId = "sch_1", name = "Kelas 10B", grade = "10", studentCount = 18, accountId = "acc_1", createdAt = 0L),
        ),
    )

    fun error(): AttendanceUiState = AttendanceUiState(error = "Gagal memuat data presensi")
}
