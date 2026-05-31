package com.azuratech.azuratime.features.aimusic.ui

/**
 * 🚀 AiMusicUiEvent.kt (v3.2.0-ai-native)
 */
sealed class AiMusicUiEvent {
    data class MoodChanged(val mood: String) : AiMusicUiEvent()
    data class RegionChanged(val region: String) : AiMusicUiEvent()
    data object GenerateSuggestions : AiMusicUiEvent()
    data class PlayPreview(val trackName: String) : AiMusicUiEvent()
}
