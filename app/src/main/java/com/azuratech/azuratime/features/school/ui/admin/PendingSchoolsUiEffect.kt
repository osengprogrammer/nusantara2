package com.azuratech.azuratime.features.school.ui.admin

/**
 * 🚀 PENDING SCHOOLS UI EFFECT (v3.2.0-ai-native)
 * Transient events for super admin school verification.
 */
sealed class PendingSchoolsUiEffect {
    data class ShowSnackbar(val message: String) : PendingSchoolsUiEffect()
}
