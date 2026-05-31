package com.azuratech.azuratime.features.aimusic.ui

import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack

/**
 * 🚀 AiMusicUiState.kt (v3.2.0-ai-native)
 */
data class AiMusicUiState(
    val isLoading: Boolean = false,
    val suggestions: List<TraditionalMusicTrack> = emptyList(),
    val error: String? = null,
    val mood: String = "",
    val region: String = "",
)
