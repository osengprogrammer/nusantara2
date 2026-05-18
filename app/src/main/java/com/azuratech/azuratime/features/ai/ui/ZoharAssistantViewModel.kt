package com.azuratech.azuratime.features.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.ai.domain.repository.ZoharRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🤖 ZOHAR ASSISTANT VIEW MODEL (v3.2.0-ai-native)
 * Integrated with ZoharRepository and strict MVI pattern.
 */
@HiltViewModel
class ZoharAssistantViewModel @Inject constructor(
    private val zoharRepository: ZoharRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(ZoharUiState())
    val uiStateFlow: StateFlow<ZoharUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: ZoharUiEvent) {
        when (event) {
            is ZoharUiEvent.AskZohar -> handleAskZohar(event.query)
            ZoharUiEvent.ClearChat -> handleClearChat()
            ZoharUiEvent.Retry -> {
                val lastQuery = _uiStateFlow.value.query
                if (lastQuery.isNotBlank()) handleAskZohar(lastQuery)
            }
            ZoharUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun handleAskZohar(userQuestion: String) {
        viewModelScope.launch {
            _uiStateFlow.update {
                it.copy(
                    query = userQuestion,
                    isLoading = true,
                    error = null,
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
                    _uiStateFlow.update {
                        it.copy(
                            isLoading = false,
                            error = "Zohar mengalami gangguan koneksi: ${error.message}",
                        )
                    }
                }
        }
    }

    private fun handleClearChat() {
        _uiStateFlow.value = ZoharUiState()
    }
}
