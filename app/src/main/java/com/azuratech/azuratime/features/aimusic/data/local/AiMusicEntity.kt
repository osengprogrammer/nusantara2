package com.azuratech.azuratime.features.aimusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack

/**
 * 🚀 AiMusicEntity.kt (v3.2.0-ai-native)
 * Room entity for traditional music tracks.
 */
@Entity(tableName = "ai_music_tracks")
data class AiMusicEntity(
    @PrimaryKey val id: String,
    val name: String,
    val region: String,
    val instrument: String,
    val mood: String,
    val audioUrl: String?,
) {
    fun toDomain() = TraditionalMusicTrack(id, name, region, instrument, mood, audioUrl)

    companion object {
        fun fromDomain(domain: TraditionalMusicTrack) = AiMusicEntity(
            id = domain.id,
            name = domain.name,
            region = domain.region,
            instrument = domain.instrument,
            mood = domain.mood,
            audioUrl = domain.audioUrl,
        )
    }
}
