package com.azuratech.azuratime.features.session.ui

import com.azuratech.azuratime.core.data.local.SessionWithDetails

/**
 * 📅 SESSION PICKER MVI CONTRACT (v3.3.0-ai-native)
 */

data class SessionPickerUiState(
    val isLoading: Boolean = false,
    val sessions: List<SessionWithDetails> = emptyList(),
    val filteredSessions: List<SessionWithDetails> = emptyList(), // 🔥 Search support
    val searchQuery: String = "", // 🔥 Search support
    val error: String? = null,
)

sealed class SessionPickerUiEvent {
    data class LoadSessions(val schoolId: String) : SessionPickerUiEvent()
    data class SelectSession(val sessionId: String) : SessionPickerUiEvent()
    data class UpdateSearchQuery(val query: String) : SessionPickerUiEvent() // 🔥 Enterprise Search
    object Refresh : SessionPickerUiEvent()
}

sealed class SessionPickerUiEffect {
    data class NavigateToScanner(val sessionId: String) : SessionPickerUiEffect()
    data class ShowError(val message: String) : SessionPickerUiEffect()
}
