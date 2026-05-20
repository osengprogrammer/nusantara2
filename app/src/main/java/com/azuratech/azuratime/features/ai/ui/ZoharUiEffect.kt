package com.azuratech.azuratime.features.ai.ui

/**
 * 🤖 ZOHAR UI EFFECT (v3.2.0-ai-native)
 */
sealed class ZoharUiEffect {
    data class ShowToast(val message: String) : ZoharUiEffect()
}
