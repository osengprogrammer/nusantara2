package com.azuratech.azuratime.features.account.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 WORKSPACE VIEW MODEL (v3.2.0-ai-native)
 * Manages workspace switching, school search, and new workspace creation.
 * 🔥 v3.1: Full Reactive SSOT with SchoolRepository and debounced search.
 */
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val repository: SchoolWorkspaceRepository,
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _statusFlow = MutableStateFlow<WorkspaceStatus>(WorkspaceStatus.Idle)
    private val _searchQueryFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val _searchResultsFlow = _searchQueryFlow
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.length < 3) {
                emptyList<Map<String, Any>>()
            } else {
                repository.searchSchools(query).getOrNull() ?: emptyList()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _accessRequestsFlow = sessionManager.currentAccountIdFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            accessRequestRepository.observeRequestsByAccountFlow(accountId)
                .map { result ->
                    result.getOrNull()?.map { it.toProfile() } ?: emptyList()
                }
        }

    val uiStateFlow: StateFlow<WorkspaceUiState> = combine(
        _statusFlow,
        _searchQueryFlow,
        _searchResultsFlow,
        _accessRequestsFlow,
    ) { status, query, results, requests ->
        WorkspaceUiState(
            status = status,
            searchQuery = query,
            searchResults = results,
            accessRequests = requests,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkspaceUiState())

    private val currentAccountId: String get() = sessionManager.getCurrentAccountId() ?: ""

    fun onEvent(event: WorkspaceUiEvent) {
        when (event) {
            is WorkspaceUiEvent.ChangeWorkspace -> changeWorkspace(event.accountId, event.newSchoolId, event.newSchoolName)
            is WorkspaceUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is WorkspaceUiEvent.SendJoinRequest -> sendJoinRequest(event.accountId, event.schoolId, event.schoolName)
            is WorkspaceUiEvent.LeaveSchool -> leaveSchool(event.schoolId)
            is WorkspaceUiEvent.CreateNewSchool -> createNewSchool(event.accountId, event.accountEmail, event.schoolName)
            is WorkspaceUiEvent.FinalizeSetup -> finalizeSetup(event.schoolId)
            is WorkspaceUiEvent.UpdateSchoolName -> updateSchoolName(event.schoolId, event.accountId, event.newName, event.onSuccess, event.onError)
            WorkspaceUiEvent.ResetState -> resetState()
        }
    }

    private fun changeWorkspace(accountId: String, newSchoolId: String, newSchoolName: String) {
        viewModelScope.launch {
            _statusFlow.value = WorkspaceStatus.Switching
            try {
                sessionManager.saveActiveSchoolId(newSchoolId)
                repository.switchWorkspace(accountId, newSchoolId)
                syncManager.enqueueAccountSync(accountId)
                _statusFlow.value = WorkspaceStatus.Success(newSchoolName)
            } catch (e: Exception) {
                _statusFlow.value = WorkspaceStatus.Error("Failed to switch workspace: ${e.message}")
            }
        }
    }

    private fun resetState() {
        _statusFlow.value = WorkspaceStatus.Idle
    }

    private fun updateSearchQuery(query: String) {
        _searchQueryFlow.value = query
    }

    private fun sendJoinRequest(accountId: String, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _statusFlow.value = WorkspaceStatus.Switching
            accessRequestRepository.submitRequest(accountId, schoolId, schoolName)
                .onSuccess { _statusFlow.value = WorkspaceStatus.RequestSent(schoolName) }
                .onFailure { _statusFlow.value = WorkspaceStatus.RequestFailed(it.message) }
        }
    }

    private fun leaveSchool(schoolId: String) {
        viewModelScope.launch {
            accessRequestRepository.cancelRequest(currentAccountId, schoolId)
        }
    }

    private fun createNewSchool(accountId: String, @Suppress("UNUSED_PARAMETER") accountEmail: String, schoolName: String) {
        viewModelScope.launch {
            _statusFlow.value = WorkspaceStatus.Switching
            schoolRepository.createSchool(accountId, schoolName, "Asia/Jakarta")
                .onSuccess { _statusFlow.value = WorkspaceStatus.Success(schoolName) }
                .onFailure { _statusFlow.value = WorkspaceStatus.Error(it.message ?: "Failed to create school") }
        }
    }

    private fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            schoolRepository.updateSchoolDetails(schoolId, name = null, timezone = null)
        }
    }

    private fun updateSchoolName(schoolId: String, @Suppress("UNUSED_PARAMETER") accountId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _statusFlow.value = WorkspaceStatus.Switching
            schoolRepository.updateSchoolDetails(schoolId, name = newName.trim(), timezone = null)
                .onSuccess {
                    _statusFlow.value = WorkspaceStatus.Idle
                    onSuccess()
                }
                .onFailure {
                    _statusFlow.value = WorkspaceStatus.Error(it.message ?: "Failed to change school name")
                    onError(it.message ?: "Failed to change school name")
                }
        }
    }
}
