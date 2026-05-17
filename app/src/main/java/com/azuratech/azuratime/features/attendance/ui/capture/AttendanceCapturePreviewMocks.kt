package com.azuratech.azuratime.features.attendance.ui.capture

object AttendanceCapturePreviewMocks {
    fun scanning(): AttendanceCaptureUiState = AttendanceCaptureUiState(
        isScanning = true,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
    )

    fun success(): AttendanceCaptureUiState = AttendanceCaptureUiState(
        isScanning = false,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
        studentProfile = StudentProfile(id = "stu_1", name = "Budi Santoso", alreadyCheckedIn = false),
    )

    fun error(): AttendanceCaptureUiState = AttendanceCaptureUiState(
        isScanning = false,
        cameraPermissionGranted = true,
        activeClassName = "Kelas 10A",
        error = "Wajah Tidak Dikenal",
    )
}
