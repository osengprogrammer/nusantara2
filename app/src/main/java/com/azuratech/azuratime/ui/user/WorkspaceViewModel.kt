package com.azuratech.azuratime.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.data.local.toProfile
import com.azuratech.azuratime.data.repo.AccessRequestRepository
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.StaffAccountRepository
import com.azuratech.azuratime.data.repo.WorkspaceRepository
import com.azuratech.azuratime.domain.model.AccessRequestProfile
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
    private val repository: WorkspaceRepository,
    private val userRepository: StaffAccountRepository,
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

    private val currentUserId: String get() = sessionManager.getCurrentUserId() ?: ""

    /**
     * Berpindah workspace sekolah aktif.
     * Menghapus data tenant lama dan mengunduh data tenant baru (Faces, Classes, Records).
     */
    fun changeWorkspace(userId: String, newSchoolId: String, newSchoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                // 1. Update SessionManager agar DAO lain langsung tahu sekolah mana yang aktif
                sessionManager.saveActiveSchoolId(newSchoolId)

                // 2. Update Context
                repository.switchWorkspace(userId, newSchoolId)

                // 3. Sync User agar Role/Membership terbaru masuk ke Room (Local-First)
                syncManager.enqueueProfileSync(userId)

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

    fun sendJoinRequest(userId: String, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            accessRequestRepository.submitRequest(userId, schoolId, schoolName)
                .onSuccess { _uiState.value = WorkspaceState.RequestSent(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.RequestFailed(it.message) }
        }
    }

    fun leaveSchool(schoolId: String) {
        viewModelScope.launch {
            accessRequestRepository.cancelRequest(currentUserId, schoolId)
        }
    }

    // =====================================================
    // 🏗️ CREATION & SETUP
    // =====================================================

    fun createNewSchool(userId: String, userEmail: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            schoolRepository.createSchool(userId, schoolName, "Asia/Jakarta")
                .onSuccess { _uiState.value = WorkspaceState.Success(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.Error(it.message ?: "Gagal membuat sekolah") }
        }
    }

    fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            schoolRepository.updateSchoolDetails(schoolId, name = null, timezone = null) // Triggers status change if needed
        }
    }

    fun updateSchoolName(schoolId: String, userId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
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
     * Observe Access Requests for the current user (SSOT Stream)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val accessRequests: StateFlow<List<AccessRequestProfile>> =
        sessionManager.currentUserIdFlow.filterNotNull()
            .flatMapLatest { userId ->
                accessRequestRepository.observeRequestsByUser(userId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
