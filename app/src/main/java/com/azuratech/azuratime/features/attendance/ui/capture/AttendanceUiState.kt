package com.azuratech.azuratime.features.attendance.ui.capture

sealed class AttendanceUiState {
    // Initial state, scanner is active and searching for a face.
    data object Idle : AttendanceUiState()

    // A face has been detected and is being processed by the ViewModel.
    // The UI should show a loading indicator.
    data object Processing : AttendanceUiState()

    // A face was successfully matched.
    data class Success(
        val name: String,
        val alreadyCheckedIn: Boolean
    ) : AttendanceUiState()

    // An error occurred (e.g., face not recognized, system error).
    data class Error(
        val message: String
    ) : AttendanceUiState()
}

sealed class AttendanceSideEffect {
    data class Speak(val message: String) : AttendanceSideEffect()
    // In a real app, you might have different sounds for success/error
    // data class PlaySound(val soundId: Int) : AttendanceSideEffect()
    data object NavigateBack : AttendanceSideEffect()
}
