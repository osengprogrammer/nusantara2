package com.azuratech.azuratime.features.aimusic.data.remote

import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack

/**
 * 🚀 AiMusicApi.kt (v3.2.0-ai-native)
 * Remote API stub for AI Music.
 */
interface AiMusicApi {
    suspend fun fetchSuggestions(mood: String?, region: String?): List<TraditionalMusicTrack>
}
