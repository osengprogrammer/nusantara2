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
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ WORKSPACE VIEW MODEL
 * Mengelola perpindahan antar sekolah, pencarian sekolah, dan pembuatan workspace baru.
 * 🔥 v3.1: Full Reactive SSOT dengan SchoolRepository dan debounced search.
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

    sealed class WorkspaceState {
        object Idle : WorkspaceState()
        object Switching : WorkspaceState()
        data class Success(val schoolName: String) : WorkspaceState()
        data class RequestSent(val schoolName: String) : WorkspaceState()
        data class RequestFailed(val message: String?) : WorkspaceState()
        data class Error(val message: String) : WorkspaceState()
    }

    private val _uiStateFlow = MutableStateFlow<WorkspaceState>(WorkspaceState.Idle)
    val uiStateFlow: StateFlow<WorkspaceState> = _uiStateFlow.asStateFlow()

    private val currentAccountId: String get() = sessionManager.getCurrentAccountId() ?: ""

    /**
     * Berpindah workspace sekolah aktif.
     */
    fun changeWorkspace(accountId: String, newSchoolId: String, newSchoolName: String) {
        viewModelScope.launch {
            _uiStateFlow.value = WorkspaceState.Switching
            try {
                sessionManager.saveActiveSchoolId(newSchoolId)
                repository.switchWorkspace(accountId, newSchoolId)
                syncManager.enqueueProfileSync(accountId)
                _uiStateFlow.value = WorkspaceState.Success(newSchoolName)
            } catch (e: Exception) {
                _uiStateFlow.value = WorkspaceState.Error("Gagal pindah workspace: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiStateFlow.value = WorkspaceState.Idle
    }

    // =====================================================
    // 🔍 SCHOOL DISCOVERY & JOIN (REACTIVE)
    // =====================================================
    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow: StateFlow<String> = _searchQueryFlow.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val schoolSearchResultsFlow: StateFlow<List<Map<String, Any>>> = _searchQueryFlow
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.length < 3) {
                emptyList<Map<String, Any>>()
            } else {
                repository.searchSchools(query).getOrNull() ?: emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQueryFlow.value = query
    }

    fun sendJoinRequest(accountId: String, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _uiStateFlow.value = WorkspaceState.Switching
            accessRequestRepository.submitRequest(accountId, schoolId, schoolName)
                .onSuccess { _uiStateFlow.value = WorkspaceState.RequestSent(schoolName) }
                .onFailure { _uiStateFlow.value = WorkspaceState.RequestFailed(it.message) }
        }
    }

    fun leaveSchool(schoolId: String) {
        viewModelScope.launch {
            accessRequestRepository.cancelRequest(currentAccountId, schoolId)
        }
    }

    // =====================================================
    // 🏗️ CREATION & SETUP
    // =====================================================

    fun createNewSchool(accountId: String, @Suppress("UNUSED_PARAMETER") accountEmail: String, schoolName: String) {
        viewModelScope.launch {
            _uiStateFlow.value = WorkspaceState.Switching
            schoolRepository.createSchool(accountId, schoolName, "Asia/Jakarta")
                .onSuccess { _uiStateFlow.value = WorkspaceState.Success(schoolName) }
                .onFailure { _uiStateFlow.value = WorkspaceState.Error(it.message ?: "Gagal membuat sekolah") }
        }
    }

    fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            schoolRepository.updateSchoolDetails(schoolId, name = null, timezone = null)
        }
    }

    fun updateSchoolName(schoolId: String, @Suppress("UNUSED_PARAMETER") accountId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiStateFlow.value = WorkspaceState.Switching
            schoolRepository.updateSchoolDetails(schoolId, name = newName.trim(), timezone = null)
                .onSuccess {
                    _uiStateFlow.value = WorkspaceState.Idle
                    onSuccess()
                }
                .onFailure {
                    _uiStateFlow.value = WorkspaceState.Error(it.message ?: "Gagal mengubah nama sekolah")
                    onError(it.message ?: "Gagal mengubah nama sekolah")
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val accessRequestsFlow: StateFlow<List<AccessRequestProfile>> =
        sessionManager.currentAccountIdFlow.filterNotNull()
            .flatMapLatest { accountId ->
                accessRequestRepository.observeRequestsByAccount(accountId)
                    .map { result ->
                        result.getOrNull()?.map { it.toProfile() } ?: emptyList()
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
