package com.azuratech.azuratime.features.student.ui.bulk

/**
 * 📝 REGISTER UI EFFECT (v3.2.0-ai-native)
 */
sealed class RegisterUiEffect {
    data class ShowToast(val message: String) : RegisterUiEffect()
}
