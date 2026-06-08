package com.azuratech.azuratime.features.attendance.ui.capture

import com.azuratech.azuratime.features.student.domain.model.StudentProfile

/**
 * 📸 ATTENDANCE CHECK-IN UI STATE
 * v3.2.0-ai-native compliant
 */
data class AttendanceCheckInUiState(
    val scanMode: ScanMode = ScanMode.Face,
    val isScanning: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val scannedResult: String? = null,
    val studentProfile: StudentProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeClassName: String = "",
    val activeClassId: String? = null,
    val activeSchoolId: String? = null,
    val isAlreadyCheckedIn: Boolean = false,
    val isWithinGeofence: Boolean = true, // 🔥 AI Native: Geofence security flag
    val geofenceEntity: com.azuratech.azuratime.features.school.data.local.GpsGeofenceEntity? = null,
)

enum class ScanMode {
    Barcode, Face, Manual
}
