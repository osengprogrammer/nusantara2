package com.azuratech.azuratime.features.aimusic.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack
import com.azuratech.azuratime.features.aimusic.domain.repository.AiMusicRepository
import javax.inject.Inject

/**
 * 🚀 GenerateMusicSuggestionUseCase.kt (v3.2.0-ai-native)
 * Single responsibility use case for generating music suggestions.
 */
class GenerateMusicSuggestionUseCase @Inject constructor(
    private val repository: AiMusicRepository,
) {
    suspend operator fun invoke(mood: String?, region: String?): Result<List<TraditionalMusicTrack>> {
        return repository.getSuggestions(mood, region)
    }
}
