package com.azuratech.azuratime.features.reporting.ui.integrity

/**
 * 🏰 DATA INTEGRITY UI EVENT
 * v3.2.0-ai-native compliant
 */
sealed class DataIntegrityUiEvent {
    data class ResolveConflict(val conflictId: String, val useCloud: Boolean) : DataIntegrityUiEvent()
    object RefreshIntegrity : DataIntegrityUiEvent()
    object ClearError : DataIntegrityUiEvent()
    data class ViewIncompleteProfiles(val type: String) : DataIntegrityUiEvent()
}
