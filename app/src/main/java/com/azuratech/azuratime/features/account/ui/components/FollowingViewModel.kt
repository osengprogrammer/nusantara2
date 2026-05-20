package com.azuratech.azuratime.features.account.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚥 FOLLOWING VIEW MODEL (v3.2.0-ai-native)
 * Strict MVI implementation for Facebook-style connection & class assignment.
 */
@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(FollowingUiState())
    val uiStateFlow: StateFlow<FollowingUiState> = _uiStateFlow.asStateFlow()

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        onEvent(FollowingUiEvent.LoadData)
    }

    fun onEvent(event: FollowingUiEvent) {
        when (event) {
            is FollowingUiEvent.SearchByEmail -> handleSearchByEmail(event.email)
            is FollowingUiEvent.SendConnectionRequest -> handleSendConnectionRequest(event.targetAccountId)
            is FollowingUiEvent.AcceptRequest -> handleAcceptRequest(event.senderId)
            is FollowingUiEvent.DeclineRequest -> handleDeclineRequest(event.senderId)
            is FollowingUiEvent.SelectFriendForAssignment -> _uiStateFlow.update { it.copy(selectedFriendForAssignment = event.friend) }
            is FollowingUiEvent.AssignClasses -> handleAssignClasses(event.targetId, event.classIds)
            FollowingUiEvent.LoadData -> loadAllData()
            FollowingUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            FollowingUiEvent.NavigateBack -> { /* Handled by screen navigation */ }
        }
    }

    private fun loadAllData() {
        observePendingRequests()
        observeConnections()
        loadAvailableClasses()
    }

    private fun handleSearchByEmail(email: String) {
        if (email.isBlank()) {
            _uiStateFlow.update { it.copy(error = "Email tidak boleh kosong.") }
            return
        }
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, error = null, searchQuery = email) }
            accountRepository.searchAccounts(email)
                .onSuccess { results ->
                    _uiStateFlow.update {
                        it.copy(isLoading = false, results = results.filter { acc -> acc.accountId != currentUserId })
                    }
                    if (results.isEmpty()) _uiStateFlow.update { it.copy(error = "Akun tidak ditemukan.") }
                }
                .onFailure { error -> _uiStateFlow.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    private fun handleSendConnectionRequest(targetAccountId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isProcessing = true) }
            accountRepository.sendConnectionRequest(uid, targetAccountId)
                .onSuccess { _uiStateFlow.update { it.copy(isProcessing = false, error = "Permintaan terkirim!") } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isProcessing = false, error = error.message) } }
        }
    }

    private fun handleAcceptRequest(senderId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isProcessing = true) }
            accountRepository.acceptConnectionRequest(uid, senderId)
                .onSuccess { _uiStateFlow.update { it.copy(isProcessing = false, error = "Berhasil terhubung!") } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isProcessing = false, error = error.message) } }
        }
    }

    private fun handleDeclineRequest(senderId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isProcessing = true) }
            accountRepository.declineConnectionRequest(uid, senderId)
                .onSuccess { _uiStateFlow.update { it.copy(isProcessing = false, error = "Permintaan ditolak.") } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isProcessing = false, error = error.message) } }
        }
    }

    private fun handleAssignClasses(targetId: String, classIds: List<String>) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isProcessing = true) }
            accountRepository.assignClassToConnection(targetId, schoolId, classIds)
                .onSuccess { _uiStateFlow.update { it.copy(isProcessing = false, error = "Berhasil memberikan akses kelas!", selectedFriendForAssignment = null) } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isProcessing = false, error = error.message) } }
        }
    }

    private fun observePendingRequests() {
        val uid = currentUserId ?: return
        accountRepository.observePendingRequests(uid)
            .onEach { result ->
                result.onSuccess { requests -> _uiStateFlow.update { it.copy(pendingRequests = requests) } }
            }
            .launchIn(viewModelScope)
    }

    private fun observeConnections() {
        val uid = currentUserId ?: return
        accountRepository.observeConnections(uid)
            .onEach { result ->
                result.onSuccess { connections -> _uiStateFlow.update { it.copy(connections = connections) } }
            }
            .launchIn(viewModelScope)
    }

    private fun loadAvailableClasses() {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        schoolRepository.observeClasses(schoolId)
            .onEach { result ->
                result.onSuccess { classes -> _uiStateFlow.update { it.copy(availableClasses = classes) } }
            }
            .launchIn(viewModelScope)
    }
}
