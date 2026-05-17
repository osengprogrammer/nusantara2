package com.azuratech.azuratime.features.attendance.ui.capture

sealed class AttendanceCaptureUiEvent {
    data class StartScan(val accountEmail: String) : AttendanceCaptureUiEvent()
    data class BarcodeDetected(val code: String) : AttendanceCaptureUiEvent()
    data class FaceDetected(val embedding: FloatArray) : AttendanceCaptureUiEvent()
    data object Retry : AttendanceCaptureUiEvent()
    data class GrantPermission(val granted: Boolean) : AttendanceCaptureUiEvent()
}

sealed class AttendanceSideEffect {
    data class Speak(val message: String) : AttendanceSideEffect()
    data object NavigateBack : AttendanceSideEffect()
}
