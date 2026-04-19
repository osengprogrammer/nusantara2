package com.azuratech.azuratime.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repository.AdminRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.admin.usecase.AdminUseCase
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
    private val adminUseCase: AdminUseCase,
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

    private val _teachersList = MutableStateFlow<List<UserEntity>>(emptyList())
    val teachersList: StateFlow<List<UserEntity>> = _teachersList.asStateFlow()

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
                adminUseCase.approveMembership(targetUserId, schoolId, schoolName, role, assignedClassIds)
                _uiState.value = AdminUiState.Success("Akses $role diberikan!")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal menyetujui: ${e.localizedMessage}")
            }
        }
    }

    fun revokeTeacherAccess(targetUserId: String, currentAdminSchoolId: String) {
        viewModelScope.launch {
            try {
                adminUseCase.revokeTeacherAccessFromWorkspace(targetUserId, currentAdminSchoolId)
                _uiState.value = AdminUiState.Success("Akses dicabut.")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() { _uiState.value = AdminUiState.Idle }

    fun inviteTeacherByEmail(teacherEmail: String, currentAdmin: UserEntity) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val schoolId = currentAdmin.activeSchoolId ?: return@launch
                val schoolName = currentAdmin.memberships[schoolId]?.schoolName ?: ""
                adminUseCase.inviteTeacherToWorkspace(schoolId, schoolName, teacherEmail, "TEACHER")
                _uiState.value = AdminUiState.Success("Undangan dikirim ke $teacherEmail")
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Gagal mengundang: ${e.localizedMessage}")
            }
        }
    }
}