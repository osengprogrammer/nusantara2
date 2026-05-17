package com.azuratech.azuratime.features.attendance.ui.capture

/**
 * 📸 ATTENDANCE CHECK-IN UI EVENT
 * v3.2.0-ai-native compliant
 */
sealed class AttendanceCheckInUiEvent {
    data class StartScan(val accountEmail: String, val mode: ScanMode = ScanMode.Face) : AttendanceCheckInUiEvent()
    data class BarcodeDetected(val code: String) : AttendanceCheckInUiEvent()
    data class FaceMatched(val embedding: FloatArray) : AttendanceCheckInUiEvent()
    object ManualEntryConfirmed : AttendanceCheckInUiEvent()
    object Retry : AttendanceCheckInUiEvent()
    data class GrantPermission(val granted: Boolean) : AttendanceCheckInUiEvent()
    object NavigateBack : AttendanceCheckInUiEvent()
}

sealed class AttendanceSideEffect {
    data class Speak(val message: String) : AttendanceSideEffect()
    object NavigateBack : AttendanceSideEffect()
}
