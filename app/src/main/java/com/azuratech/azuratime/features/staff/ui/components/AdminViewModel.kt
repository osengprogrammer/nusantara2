package com.azuratech.azuratime.features.staff.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import com.azuratech.azuratime.features.staff.data.local.StaffAccountEntity
import com.azuratech.azuratime.features.staff.data.repo.AdminRepository
import com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AdminViewModel @Inject constructor(
    application: Application,
    database: AppDatabase,
    private val repository: AdminRepository,
    private val userRepository: StaffAccountRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    // 🔥 Stream kelas berdasarkan sekolah aktif (untuk dialog approval)
    @OptIn(ExperimentalCoroutinesApi::class)
    val classes: StateFlow<List<ClassEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> database.classDao().observeClassesBySchool(schoolId) }
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    sealed class AdminUiState {
        object Idle : AdminUiState()
        object Loading : AdminUiState()
        data class Success(val message: String) : AdminUiState()
        data class Error(val message: String) : AdminUiState()
    }

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _teachersList = MutableStateFlow<List<StaffAccountEntity>>(emptyList())
    val teachersList: StateFlow<List<StaffAccountEntity>> = _teachersList.asStateFlow()

    fun startObservingTeachers(currentAdminSchoolId: String) {
        viewModelScope.launch {
            repository.observeUsersForSchool(currentAdminSchoolId).collect { users ->
                _teachersList.value = users
            }
        }
    }

    /**
     * 🔥 IMPROVED APPROVAL: Now supports assigning classes immediately.
     */
    fun approveFollower(
        targetUserId: String,
        schoolId: String,
        schoolName: String,
        role: String,
        assignedClassIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                userRepository.approveMembership(targetUserId, schoolId, schoolName, role, assignedClassIds)
                _uiState.value = AdminUiState.Success("Akses $role diberikan!")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal menyetujui: ${e.localizedMessage}")
            }
        }
    }

    fun revokeTeacherAccess(targetUserId: String, currentAdminSchoolId: String) {
        viewModelScope.launch {
            try {
                userRepository.revokeMembership(targetUserId, currentAdminSchoolId)
                _uiState.value = AdminUiState.Success("Akses dicabut.")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() { _uiState.value = AdminUiState.Idle }

    fun inviteTeacherByEmail(teacherEmail: String, currentAdmin: StaffAccountEntity) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                userRepository.sendFriendRequest(currentAdmin.userId, currentAdmin.name, currentAdmin.email, teacherEmail)
                _uiState.value = AdminUiState.Success("Undangan dikirim ke $teacherEmail")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal mengundang: ${e.localizedMessage}")
            }
        }
    }
}