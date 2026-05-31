package com.azuratech.azuratime.features.aimusic.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🚀 AiMusicDao.kt (v3.2.0-ai-native)
 */
@Dao
interface AiMusicDao {
    @Query("SELECT * FROM ai_music_tracks")
    fun getAllTracks(): Flow<List<AiMusicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AiMusicEntity)

    @Query("SELECT * FROM ai_music_tracks WHERE region = :region")
    suspend fun getTracksByRegion(region: String): List<AiMusicEntity>
}
