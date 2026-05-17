package com.azuratech.azuratime.features.biometric.ui.enroll

/**
 * 🧬 BIOMETRIC ENROLLMENT UI EVENT (v3.2.0-ai-native)
 */
sealed class BiometricEnrollmentUiEvent {
    data class StartCapture(val studentId: String) : BiometricEnrollmentUiEvent()
    data class FaceCaptured(val embedding: FloatArray) : BiometricEnrollmentUiEvent()
    data class SyncBiometric(val studentId: String) : BiometricEnrollmentUiEvent()
    data class DeleteBiometric(val studentId: String) : BiometricEnrollmentUiEvent()
    data object Retry : BiometricEnrollmentUiEvent()
    data object ClearError : BiometricEnrollmentUiEvent()
    data object NavigateBack : BiometricEnrollmentUiEvent()
    data class GrantCameraPermission(val granted: Boolean) : BiometricEnrollmentUiEvent()
}
