package com.azuratech.azuratime.features.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.ai.domain.repository.ZoharRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🤖 ZOHAR ASSISTANT VIEW MODEL (v3.2.0-ai-native)
 * Optimized with Effect-Driven MVI pattern.
 */
@HiltViewModel
class ZoharAssistantViewModel @Inject constructor(
    private val zoharRepository: ZoharRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(ZoharUiState())
    val uiStateFlow: StateFlow<ZoharUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<ZoharUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: ZoharUiEvent) {
        when (event) {
            is ZoharUiEvent.AskZohar -> handleAskZohar(event.query)
            ZoharUiEvent.ClearChat -> handleClearChat()
            ZoharUiEvent.Retry -> {
                val lastQuery = _uiStateFlow.value.query
                if (lastQuery.isNotBlank()) handleAskZohar(lastQuery)
            }
            ZoharUiEvent.ClearError -> { /* Handled via Effects in UI */ }
        }
    }

    private fun handleAskZohar(userQuestion: String) {
        viewModelScope.launch {
            _uiStateFlow.update {
                it.copy(
                    query = userQuestion,
                    isLoading = true,
                    conversationHistory = it.conversationHistory + ChatMessage(ChatRole.USER, userQuestion),
                )
            }

            val schoolId = sessionManager.getActiveSchoolId() ?: ""

            zoharRepository.askZohar(userQuestion, schoolId)
                .onSuccess { response ->
                    _uiStateFlow.update {
                        it.copy(
                            response = response,
                            isLoading = false,
                            conversationHistory = it.conversationHistory + ChatMessage(ChatRole.ZOHAR, response),
                        )
                    }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(ZoharUiEffect.ShowToast("Zohar mengalami gangguan koneksi: ${error.message}"))
                }
        }
    }

    private fun handleClearChat() {
        _uiStateFlow.value = ZoharUiState()
    }
}
