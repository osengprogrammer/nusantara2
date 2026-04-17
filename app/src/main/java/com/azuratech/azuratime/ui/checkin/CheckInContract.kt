package com.azuratech.azuratime.ui.checkin

sealed class CheckInUiState {
    // Initial state, scanner is active and searching for a face.
    data object Idle : CheckInUiState()

    // A face has been detected and is being processed by the ViewModel.
    // The UI should show a loading indicator.
    data object Processing : CheckInUiState()

    // A face was successfully matched.
    data class Success(
        val name: String,
        val alreadyCheckedIn: Boolean
    ) : CheckInUiState()

    // An error occurred (e.g., face not recognized, system error).
    data class Error(
        val message: String
    ) : CheckInUiState()
}

sealed class CheckInSideEffect {
    data class Speak(val message: String) : CheckInSideEffect()
    // In a real app, you might have different sounds for success/error
    // data class PlaySound(val soundId: Int) : CheckInSideEffect()
    data object NavigateBack : CheckInSideEffect()
}
