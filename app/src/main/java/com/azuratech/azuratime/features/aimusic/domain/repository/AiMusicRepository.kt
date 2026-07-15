package com.azuratech.azuratime.features.aimusic.domain.repository

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack

/**
 * 🚀 AiMusicRepository.kt (v3.2.0-ai-native)
 * Interface for AI Music repository.
 */
interface AiMusicRepository {
    suspend fun getSuggestions(mood: String?, region: String?): Result<List<TraditionalMusicTrack>>
    suspend fun saveTrack(track: TraditionalMusicTrack): Result<Unit>
}
