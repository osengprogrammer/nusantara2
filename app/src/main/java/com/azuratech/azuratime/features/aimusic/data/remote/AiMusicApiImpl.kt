package com.azuratech.azuratime.features.aimusic.data.remote

import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack
import javax.inject.Inject

/**
 * 🚀 AiMusicApiImpl.kt (v3.2.0-ai-native)
 */
class AiMusicApiImpl @Inject constructor() : AiMusicApi {
    override suspend fun fetchSuggestions(mood: String?, region: String?): List<TraditionalMusicTrack> {
        // Stub: Always returns empty list unless handled in repository (ML stub)
        return emptyList()
    }
}
