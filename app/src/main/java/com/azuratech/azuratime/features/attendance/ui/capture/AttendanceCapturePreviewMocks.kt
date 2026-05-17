package com.azuratech.azuratime.features.attendance.ui.capture

import com.azuratech.azuratime.features.student.domain.model.StudentProfile

/**
 * 📸 ATTENDANCE CHECK-IN PREVIEW MOCKS
 */
object AttendanceCapturePreviewMocks {
    fun scanning(): AttendanceCheckInUiState = AttendanceCheckInUiState(
        isScanning = true,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
    )

    fun success(): AttendanceCheckInUiState = AttendanceCheckInUiState(
        isScanning = false,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
        studentProfile = StudentProfile(
            studentId = "stu_1",
            name = "Budi Santoso",
            schoolId = "sch_1",
        ),
        isAlreadyCheckedIn = false,
    )

    fun error(): AttendanceCheckInUiState = AttendanceCheckInUiState(
        isScanning = false,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
        error = "Identitas Tidak Dikenal",
    )
}
