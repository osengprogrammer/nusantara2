package com.azuratech.azuratime.features.reporting.ui.integrity

/**
 * 🚀 DATA INTEGRITY UI EFFECT
 * Transient events for system health monitoring and conflict resolution.
 */
sealed class DataIntegrityUiEffect {
    data class ShowSnackbar(val message: String) : DataIntegrityUiEffect()
}
