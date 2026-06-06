package com.azuratech.azuratime.features.biometric.ui.enroll

/**
 * 🧬 BIOMETRIC UI EFFECT
 * Transient events for biometric enrollment.
 */
sealed class BiometricUiEffect {
    data class ShowSnackbar(val message: String) : BiometricUiEffect()
}
