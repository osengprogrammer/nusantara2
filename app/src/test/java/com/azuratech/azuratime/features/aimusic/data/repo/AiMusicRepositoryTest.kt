package com.azuratech.azuratime.features.aimusic.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.aimusic.data.local.AiMusicDao
import com.azuratech.azuratime.features.aimusic.data.remote.AiMusicApi
import com.azuratech.azuratime.features.aimusic.domain.repository.AiMusicRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 🚀 AiMusicRepositoryTest.kt (Phase 24)
 * Verifies rule-based filtering logic in AiMusicRepositoryImpl.
 */
class AiMusicRepositoryTest {

    private lateinit var dao: AiMusicDao
    private lateinit var api: AiMusicApi
    private lateinit var repository: AiMusicRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        api = mockk(relaxed = true)
        repository = AiMusicRepositoryImpl(dao, api)
    }

    @Test
    fun `getSuggestions with region Jawa returns only Javanese tracks`() = runTest {
        // WHEN
        val result = repository.getSuggestions(mood = null, region = "Jawa")

        // THEN
        assertTrue(result is Result.Success)
        val tracks = (result as Result.Success).data
        assertTrue(tracks.isNotEmpty())
        tracks.forEach {
            assertTrue(it.region.contains("Jawa", ignoreCase = true))
        }
    }

    @Test
    fun `getSuggestions with mood Semangat returns matching tracks`() = runTest {
        // WHEN
        val result = repository.getSuggestions(mood = "Semangat", region = null)

        // THEN
        assertTrue(result is Result.Success)
        val tracks = (result as Result.Success).data
        assertTrue(tracks.isNotEmpty())
        tracks.forEach {
            assertTrue(it.mood.contains("Semangat", ignoreCase = true))
        }
    }

    @Test
    fun `getSuggestions with no matches returns default list`() = runTest {
        // WHEN
        val result = repository.getSuggestions(mood = "Metal", region = "Mars")

        // THEN
        assertTrue(result is Result.Success)
        val tracks = (result as Result.Success).data
        assertEquals(3, tracks.size) // shufffled take(3)
    }
}
