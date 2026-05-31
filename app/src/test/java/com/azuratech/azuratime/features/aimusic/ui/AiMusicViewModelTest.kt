package com.azuratech.azuratime.features.aimusic.ui

import app.cash.turbine.test
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack
import com.azuratech.azuratime.features.aimusic.domain.usecase.GenerateMusicSuggestionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 🚀 AiMusicViewModelTest.kt (v3.2.0-ai-native)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiMusicViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: GenerateMusicSuggestionUseCase
    private lateinit var viewModel: AiMusicViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        viewModel = AiMusicViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generate suggestions success updates state and emits effect`() = runTest {
        val tracks = listOf(TraditionalMusicTrack("1", "Gamelan Jawa", "Jawa", "Gamelan", "Khidmat", null))
        coEvery { useCase(any(), any()) } returns Result.Success(tracks)

        // Start collecting effects before triggering the event
        val effectJob = launch {
            viewModel.uiEffect.test {
                assertEquals(AiMusicUiEffect.ShowToast("Suggestions updated!"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.uiState.test {
            assertEquals(AiMusicUiState(), awaitItem())

            viewModel.onEvent(AiMusicUiEvent.GenerateSuggestions)
            assertEquals(AiMusicUiState(isLoading = true), awaitItem())

            val success = awaitItem()
            assertEquals(tracks, success.suggestions)
            assertEquals(false, success.isLoading)
        }

        effectJob.join()
    }

    @Test
    fun `filtering by Jawa returns only Javanese tracks`() = runTest {
        // GIVEN
        val jawaTrack = TraditionalMusicTrack("1", "Gamelan Jawa", "Jawa", "Gamelan", "Khidmat", null)
        coEvery { useCase(any(), eq("Jawa")) } returns Result.Success(listOf(jawaTrack))

        // WHEN
        viewModel.onEvent(AiMusicUiEvent.RegionChanged("Jawa"))
        viewModel.onEvent(AiMusicUiEvent.GenerateSuggestions)

        // THEN
        viewModel.uiState.test {
            val state = awaitItem()
            if (state.isLoading) {
                val nextState = awaitItem()
                assertEquals(listOf(jawaTrack), nextState.suggestions)
            } else {
                assertEquals(listOf(jawaTrack), state.suggestions)
            }
        }
    }
}
