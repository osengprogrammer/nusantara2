package com.azuratech.azuratime.features.biometric.ui.enroll

/**
 * 🧬 BIOMETRIC PREVIEW MOCKS (v3.2.0-ai-native)
 */
object BiometricPreviewMocks {
    fun idle(): BiometricEnrollmentUiState = BiometricEnrollmentUiState(
        enrollmentStatus = EnrollmentStatus.IDLE,
    )

    fun capturing(): BiometricEnrollmentUiState = BiometricEnrollmentUiState(
        enrollmentStatus = EnrollmentStatus.CAPTURING,
        isScanning = true,
        cameraPermissionGranted = true,
    )

    fun success(): BiometricEnrollmentUiState = BiometricEnrollmentUiState(
        enrollmentStatus = EnrollmentStatus.SUCCESS,
        studentId = "stu_123",
    )

    fun error(): BiometricEnrollmentUiState = BiometricEnrollmentUiState(
        enrollmentStatus = EnrollmentStatus.FAILURE,
        error = "Gagal mendeteksi wajah. Pastikan pencahayaan cukup.",
    )
}
