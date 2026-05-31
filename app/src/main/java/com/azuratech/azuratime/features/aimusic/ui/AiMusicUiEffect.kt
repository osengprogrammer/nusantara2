package com.azuratech.azuratime.features.aimusic.ui

/**
 * 🚀 AiMusicUiEffect.kt (v3.2.0-ai-native)
 */
sealed class AiMusicUiEffect {
    data class ShowToast(val message: String) : AiMusicUiEffect()
}
