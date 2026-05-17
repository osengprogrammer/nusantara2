package com.azuratech.azuratime.features.attendance.ui.capture

data class StudentProfile(
    val id: String,
    val name: String,
    val alreadyCheckedIn: Boolean = false,
)

data class AttendanceCaptureUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = true,
    val scannedCode: String? = null,
    val studentProfile: StudentProfile? = null,
    val error: String? = null,
    val cameraPermissionGranted: Boolean = false,
    val activeClassName: String = "",
    val activeSchoolId: String? = null,
)
