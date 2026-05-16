package com.azuratech.azuratime.features.account.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.account.data.repo.SchoolWorkspaceRepository
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
    private val syncManager: SyncManager
) : ViewModel() {

    sealed class WorkspaceState {
        object Idle : WorkspaceState()
        object Switching : WorkspaceState()
        data class Success(val schoolName: String) : WorkspaceState()
        data class RequestSent(val schoolName: String) : WorkspaceState()
        data class RequestFailed(val message: String?) : WorkspaceState()
        data class Error(val message: String) : WorkspaceState()
    }

    private val _uiState = MutableStateFlow<WorkspaceState>(WorkspaceState.Idle)
    val uiState: StateFlow<WorkspaceState> = _uiState.asStateFlow()

    private val currentAccountId: String get() = sessionManager.getCurrentUserId() ?: ""

    /**
     * Berpindah workspace sekolah aktif.
     */
    fun changeWorkspace(accountId: String, newSchoolId: String, newSchoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                sessionManager.saveActiveSchoolId(newSchoolId)
                repository.switchWorkspace(accountId, newSchoolId)
                syncManager.enqueueProfileSync(accountId)
                _uiState.value = WorkspaceState.Success(newSchoolName)
            } catch (e: Exception) {
                _uiState.value = WorkspaceState.Error("Gagal pindah workspace: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = WorkspaceState.Idle
    }

    // =====================================================
    // 🔍 SCHOOL DISCOVERY & JOIN (REACTIVE)
    // =====================================================
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val schoolSearchResults: StateFlow<List<Map<String, Any>>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .map { query ->
            if (query.length < 3) emptyList()
            else repository.searchSchools(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendJoinRequest(accountId: String, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            accessRequestRepository.submitRequest(accountId, schoolId, schoolName)
                .onSuccess { _uiState.value = WorkspaceState.RequestSent(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.RequestFailed(it.message) }
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
            _uiState.value = WorkspaceState.Switching
            schoolRepository.createSchool(accountId, schoolName, "Asia/Jakarta")
                .onSuccess { _uiState.value = WorkspaceState.Success(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.Error(it.message ?: "Gagal membuat sekolah") }
        }
    }

    fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            schoolRepository.updateSchoolDetails(schoolId, name = null, timezone = null)
        }
    }

    fun updateSchoolName(schoolId: String, @Suppress("UNUSED_PARAMETER") accountId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            schoolRepository.updateSchoolDetails(schoolId, name = newName.trim(), timezone = null)
                .onSuccess {
                    _uiState.value = WorkspaceState.Idle
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = WorkspaceState.Error(it.message ?: "Gagal mengubah nama sekolah")
                    onError(it.message ?: "Gagal mengubah nama sekolah")
                }
        }
    }

    /**
     * Observe Access Requests for the current account (SSOT Stream)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val accessRequests: StateFlow<List<AccessRequestProfile>> =
        sessionManager.currentUserIdFlow.filterNotNull()
            .flatMapLatest { accountId ->
                accessRequestRepository.observeRequestsByUser(accountId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
