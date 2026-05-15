package com.azuratech.azuratime.features.school.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.features.staff.data.repo.WorkspaceRepository
import com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository
import com.azuratech.azuratime.core.domain.model.SyncStatus
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val userRepository: StaffAccountRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _accountId = MutableStateFlow(savedStateHandle.get<String>("accountId") ?: sessionManager.getCurrentUserId() ?: "")
    val accountId: StateFlow<String> = _accountId.asStateFlow()

    // 🔥 v3.1: Reactive School SSOT Migration (Phase 7.7)
    val schools: StateFlow<List<School>> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> schoolRepository.observeSchools(id) }
        .map { result ->
            if (result is Result.Success) {
                // Auto-select first school if none active (Side-effect in map for SSOT transition)
                if (result.data.isNotEmpty() && sessionManager.getActiveSchoolId() == null) {
                    selectSchool(result.data.first())
                }
                result.data
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSchoolId: StateFlow<String?> = sessionManager.activeSchoolIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.getActiveSchoolId())

    val activeSchool: StateFlow<School?> = activeSchoolId
        .flatMapLatest { id ->
            if (id != null) flow { emit(schoolRepository.getSchoolById(id)) }
            else flowOf<School?>(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSchools: StateFlow<List<School>> = schools

    // 🔥 Added Available Classes flow for selection
    val availableClasses: StateFlow<List<ClassModel>> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            schoolRepository.observeAllClassesForAccount(id).map { result ->
                if (result is Result.Success) result.data else emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAccountId(id: String) {
        if (id.isNotEmpty() && _accountId.value != id) {
            _accountId.value = id
        }
    }

    /**
     * 🔥 SELECT SCHOOL & PERSIST
     * Updates local session and cloud context through WorkspaceRepository.
     */
    fun selectSchool(school: School) {
        viewModelScope.launch {
            val userId = _accountId.value
            if (userId.isEmpty()) return@launch
            
            println("🔄 Switching school to: ${school.name} (${school.id})")
            sessionManager.saveActiveSchoolId(school.id)
            try {
                workspaceRepository.switchWorkspace(userId, school.id)
            } catch (e: Exception) {
                println("⚠️ Error switching workspace: ${e.message}")
            }
        }
    }

    fun createSchool(name: String, timezone: String, selectedClassIds: List<String> = emptyList()) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        println("💾 DEBUG: Creating school: $name with ${selectedClassIds.size} classes")
        viewModelScope.launch {
            val user = userRepository.getUserById(currentId)
            val role = user?.role ?: "USER"
            
            // 🔥 Business Rule: One account one school unless SUPER_ADMIN
            if (role != "SUPER_ADMIN" && schools.value.isNotEmpty()) {
                _uiEvent.emit(UiEvent.ShowSnackbar("❌ Gagal: Hanya Super Admin yang dapat membuat lebih dari satu sekolah."))
                return@launch
            }

            val result = schoolRepository.createSchool(currentId, name, timezone)
            if (result is Result.Success) {
                val newSchoolId = result.data
                selectedClassIds.forEach { classId ->
                    schoolRepository.assignClassToSchool(newSchoolId, classId)
                }
                
                val newSchool = schoolRepository.getSchoolById(newSchoolId)
                
                // Show Feedback
                val status = newSchool?.status ?: "PENDING"
                if (status == "ACTIVE") {
                    _uiEvent.emit(UiEvent.ShowSnackbar("🎉 Sekolah aktif! Anda adalah Admin."))
                    
                    // 🔥 Auto-select if it's the first one/active
                    if (sessionManager.getActiveSchoolId() == null) {
                        newSchool?.let { selectSchool(it) }
                    }
                } else {
                    _uiEvent.emit(UiEvent.ShowSnackbar("⏳ Menunggu verifikasi Super Admin."))
                }
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("❌ Gagal: ${result.error.message}"))
            }
        }
    }

    fun deleteSchool(id: String) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        viewModelScope.launch {
            schoolRepository.deleteSchool(id, currentId)
        }
    }

    // Deprecated but kept for backward compatibility if needed, though we should update callers
    fun addSchool(accountId: String, name: String, timezone: String) {
        setAccountId(accountId)
        createSchool(name, timezone)
    }
}
