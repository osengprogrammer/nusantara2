package com.azuratech.azuratime.features.aimusic.data.repo

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.features.aimusic.data.local.AiMusicDao
import com.azuratech.azuratime.features.aimusic.data.local.AiMusicEntity
import com.azuratech.azuratime.features.aimusic.data.remote.AiMusicApi
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack
import com.azuratech.azuratime.features.aimusic.domain.repository.AiMusicRepository
import javax.inject.Inject

/**
 * 🚀 AiMusicRepositoryImpl.kt (v3.2.0-ai-native)
 */
class AiMusicRepositoryImpl @Inject constructor(
    private val dao: AiMusicDao,
    private val api: AiMusicApi,
) : AiMusicRepository {

    private val trackDataset = listOf(
        TraditionalMusicTrack("1", "Gamelan Jawa", "Jawa", "Gamelan", "Khidmat", null),
        TraditionalMusicTrack("2", "Angklung Sunda", "Sunda", "Angklung", "Ceria", null),
        TraditionalMusicTrack("3", "Kolintang Sulawesi", "Sulawesi", "Kolintang", "Semangat", null),
        TraditionalMusicTrack("4", "Saman Gayo", "Aceh", "Vokal", "Semangat", null),
        TraditionalMusicTrack("5", "Gamelan Bali", "Bali", "Gamelan", "Semangat", null),
        TraditionalMusicTrack("6", "Sasando Rote", "NTT", "Sasando", "Khidmat", null),
        TraditionalMusicTrack("7", "Degung Sunda", "Sunda", "Gamelan", "Calm", null),
        TraditionalMusicTrack("8", "Talempong Minang", "Sumatera Barat", "Talempong", "Ceria", null),
    )

    override suspend fun getSuggestions(mood: String?, region: String?): Result<List<TraditionalMusicTrack>> {
        return try {
            // Fetch from AI API
            val aiSuggestions = api.fetchSuggestions(mood, region)

            // Fallback to local dataset if AI fails or returns empty
            var filtered = trackDataset

            if (!region.isNullOrBlank()) {
                filtered = filtered.filter { it.region.contains(region, ignoreCase = true) }
            }

            if (!mood.isNullOrBlank()) {
                filtered = filtered.filter { it.mood.contains(mood, ignoreCase = true) }
            }

            val result = (aiSuggestions + filtered).distinctBy { it.name }.shuffled()
            val finalResult = result.ifEmpty { trackDataset.shuffled().take(3) }

            // Save to local for offline support
            finalResult.forEach { saveTrack(it) }

            Result.Success(finalResult)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun saveTrack(track: TraditionalMusicTrack): Result<Unit> {
        return try {
            dao.insertTrack(AiMusicEntity.fromDomain(track))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
