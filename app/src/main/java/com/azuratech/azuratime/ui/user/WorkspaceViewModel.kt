package com.azuratech.azuratime.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repo.WorkspaceRepository
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.domain.user.usecase.SubmitSchoolAccessUseCase
import com.azuratech.azuratime.domain.user.usecase.CancelSchoolAccessUseCase
import com.azuratech.azuratime.domain.school.usecase.CreateSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.UpdateSchoolDetailsUseCase
import com.azuratech.azuratime.data.repo.AccessRequestRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuraengine.result.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ WORKSPACE VIEW MODEL
 * Mengelola perpindahan antar sekolah, pencarian sekolah, dan pembuatan workspace baru.
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
 */
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    application: Application,
    private val repository: WorkspaceRepository,
    private val userRepository: UserRepository,
    private val syncUserUseCase: SyncUserUseCase,
    private val submitRequestUseCase: SubmitSchoolAccessUseCase,
    private val cancelRequestUseCase: CancelSchoolAccessUseCase,
    private val createSchoolUseCase: CreateSchoolUseCase,
    private val updateSchoolDetailsUseCase: UpdateSchoolDetailsUseCase,
    private val accessRequestRepository: AccessRequestRepository,
    private val sessionManager: SessionManager,
    private val db: FirebaseFirestore
) : AndroidViewModel(application) {

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

                // 3. Sync User agar Role/Membership terbaru masuk ke Room
                syncUserUseCase(userId)

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
    // 🔍 SCHOOL DISCOVERY & JOIN
    // =====================================================
    private val _schoolSearchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val schoolSearchResults: StateFlow<List<Map<String, Any>>> = _schoolSearchResults.asStateFlow()

    fun searchSchools(query: String) {
        if (query.length < 3) {
            _schoolSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _schoolSearchResults.value = repository.searchSchools(query)
        }
    }

    fun sendJoinRequest(user: UserEntity, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            submitRequestUseCase(user.userId, schoolId, schoolName, "TEACHER")
                .onSuccess { _uiState.value = WorkspaceState.RequestSent(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.RequestFailed(it.message) }
        }
    }

    fun leaveSchool(schoolId: String) {
        viewModelScope.launch {
            cancelRequestUseCase(currentUserId, schoolId)
        }
    }

    // =====================================================
    // 🏗️ CREATION & SETUP
    // =====================================================

    fun createNewSchool(userId: String, userEmail: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            createSchoolUseCase(userId, schoolName)
                .onSuccess { _uiState.value = WorkspaceState.Success(schoolName) }
                .onFailure { _uiState.value = WorkspaceState.Error(it.message ?: "Gagal membuat sekolah") }
        }
    }

    fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            updateSchoolDetailsUseCase(schoolId) // Default status in Entity is ACTIVE
        }
    }

    fun updateSchoolName(schoolId: String, userId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            updateSchoolDetailsUseCase(schoolId, name = newName.trim())
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
     * Observe Access Requests for the current user
     */
    val accessRequests = accessRequestRepository.observeRequestsByUser(currentUserId)
}
// 🔥 FACTORY DIHAPUS SEPENUHNYA