package com.azuratech.azuratime.features.aimusic.data.remote

import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * 🚀 AiMusicApiImpl.kt (v3.2.0-ai-native)
 * Implemented with Gemini 1.5 Flash and kotlinx.serialization.
 */
class AiMusicApiImpl @Inject constructor() : AiMusicApi {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1beta"),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun fetchSuggestions(mood: String?, region: String?): List<TraditionalMusicTrack> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return emptyList()

        val prompt = """
            Berikan 3 rekomendasi musik tradisional Indonesia dalam format JSON.
            Mood: ${mood ?: "Any"}
            Region: ${region ?: "Any"}

            Format JSON harus berupa list object dengan field:
            - id: string unik
            - name: nama lagu/musik
            - region: asal daerah
            - instrument: instrumen utama
            - mood: mood musiknya
            - audioUrl: null

            Hanya berikan JSON array, tanpa ```json atau teks lain.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text?.trim() ?: ""

            // Clean markdown if present
            val cleanJson = responseText.removeSurrounding("```json", "```").trim()

            json.decodeFromString<List<TraditionalMusicTrack>>(cleanJson)
        } catch (e: Exception) {
            android.util.Log.e("AiMusicApi", "AI Error: ${e.message}", e)
            emptyList()
        }
    }
}
