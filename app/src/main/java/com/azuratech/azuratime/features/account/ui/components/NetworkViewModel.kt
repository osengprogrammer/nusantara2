package com.azuratech.azuratime.features.account.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚥 NETWORK VIEW MODEL (v3.2.0-ai-native)
 * Strict MVI implementation for account discovery and connections.
 */
@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(NetworkUiState())
    val uiStateFlow: StateFlow<NetworkUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: NetworkUiEvent) {
        when (event) {
            is NetworkUiEvent.SearchByEmail -> handleSearchByEmail(event.email)
            is NetworkUiEvent.SendFriendRequest -> handleSendFriendRequest(event.targetEmail)
            NetworkUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            NetworkUiEvent.NavigateBack -> { /* Handled by screen navigation */ }
        }
    }

    private fun handleSearchByEmail(email: String) {
        if (email.isBlank()) {
            _uiStateFlow.update { it.copy(error = "Email tidak boleh kosong, Dulur.") }
            return
        }

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, error = null, searchQuery = email) }
            // Placeholder: implementation needed in AccountRepository
            _uiStateFlow.update {
                it.copy(
                    isLoading = false,
                    error = "Waduh, guru dengan email $email tidak ditemukan.",
                )
            }
        }
    }

    private fun handleSendFriendRequest(targetEmail: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isSendingRequest = true) }
            // Placeholder
            _uiStateFlow.update { it.copy(isSendingRequest = false) }
        }
    }
}
