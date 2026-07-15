package com.azuratech.azuratime.features.aimusic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.features.aimusic.domain.usecase.GenerateMusicSuggestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 AiMusicViewModel.kt (v3.2.0-ai-native)
 */
@HiltViewModel
class AiMusicViewModel @Inject constructor(
    private val generateMusicSuggestionUseCase: GenerateMusicSuggestionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiMusicUiState())
    val uiState: StateFlow<AiMusicUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AiMusicUiEffect>()
    val uiEffect: SharedFlow<AiMusicUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: AiMusicUiEvent) {
        when (event) {
            is AiMusicUiEvent.MoodChanged -> _uiState.update { it.copy(mood = event.mood) }
            is AiMusicUiEvent.RegionChanged -> _uiState.update { it.copy(region = event.region) }
            is AiMusicUiEvent.GenerateSuggestions -> generateSuggestions()
            is AiMusicUiEvent.PlayPreview -> {
                viewModelScope.launch {
                    _uiEffect.emit(AiMusicUiEffect.ShowToast("Playing ${event.trackName}"))
                }
            }
        }
    }

    private fun generateSuggestions() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            generateMusicSuggestionUseCase(
                mood = _uiState.value.mood.takeIf { it.isNotBlank() },
                region = _uiState.value.region.takeIf { it.isNotBlank() },
            ).onSuccess { data ->
                _uiState.update { it.copy(isLoading = false, suggestions = data) }
                _uiEffect.emit(AiMusicUiEffect.ShowToast("Suggestions updated!"))
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
                _uiEffect.emit(AiMusicUiEffect.ShowToast("Failed: ${error.message}"))
            }
        }
    }
}
