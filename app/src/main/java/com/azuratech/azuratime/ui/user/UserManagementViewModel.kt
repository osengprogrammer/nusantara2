package com.azuratech.azuratime.ui.user

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.AttendanceConflict
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.user.usecase.UpdateUserUseCase
import com.azuratech.azuratime.domain.checkin.usecase.ResolveConflictUseCase
import com.azuratech.azuratime.domain.user.usecase.UserManagementUseCase
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.domain.user.usecase.ObserveUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ USER MANAGEMENT VIEW MODEL
 * Pengelola profil user, hak akses kelas, dan relasi antar pengajar.
 * 🔥 Refactored: Fully UseCase-driven!
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase,
    private val repository: UserRepository,
    private val sessionManager: SessionManager,
    private val updateUserUseCase: UpdateUserUseCase,
    private val resolveConflictUseCase: ResolveConflictUseCase,
    private val userManagementUseCase: UserManagementUseCase,
    private val syncUserUseCase: SyncUserUseCase,
    private val observeUserUseCase: ObserveUserUseCase
) : AndroidViewModel(application) {

    // =====================================================
    // 1. DIRI SENDIRI (Active Admin/Teacher Session)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid -> observeUserUseCase(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val assignedClassIds: StateFlow<List<String>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .combine(sessionManager.currentUserIdFlow.filterNotNull()) { schoolId, userId -> schoolId to userId }
        .flatMapLatest { (schoolId, userId) -> repository.getUserClassAccessDao().observeClassIdsForUser(userId, schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 2. JARINGAN (Explore Users in the same school)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUsersInSameSchool: StateFlow<List<UserEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            repository.getUserDao().observeAllUsers().map { users ->
                users.filter { it.memberships.containsKey(schoolId) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 3. TARGET MANAGEMENT (Managing other teachers)
    // =====================================================

    private val _selectedTargetUser = MutableStateFlow<UserEntity?>(null)
    val selectedTargetUser: StateFlow<UserEntity?> = _selectedTargetUser.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val targetAssignedClassIds: StateFlow<List<String>> = _selectedTargetUser
        .filterNotNull()
        .combine(sessionManager.activeSchoolIdFlow.filterNotNull()) { target, schoolId -> target.userId to schoolId }
        .flatMapLatest { (targetId, schoolId) -> repository.getUserClassAccessDao().observeClassIdsForUser(targetId, schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTargetUser(userId: String, name: String, email: String) {
        _selectedTargetUser.value = UserEntity(
            userId = userId,
            name = name,
            email = email
        )
    }

    // =====================================================
    // 🛠️ OPERATIONS
    // =====================================================

    fun selectActiveClass(classId: String?) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(activeClassId = classId)
            updateUserUseCase(updatedUser)
        }
    }

    fun assignClassToUser(classId: String, targetUserId: String? = null) {
        val targetId = targetUserId ?: currentUser.value?.userId ?: return
        val schoolId = currentUser.value?.activeSchoolId ?: return
        viewModelScope.launch {
            userManagementUseCase.assignClassToUser(targetId, schoolId, classId)
        }
    }

    fun removeClassAccess(classId: String, targetUserId: String? = null) {
        val targetId = targetUserId ?: currentUser.value?.userId ?: return
        val schoolId = currentUser.value?.activeSchoolId ?: return
        viewModelScope.launch {
            userManagementUseCase.removeClassAccess(targetId, classId, schoolId)
        }
    }

    // =====================================================
    // 🚪 CONFLICT RESOLUTION
    // =====================================================
    val conflicts = repository.conflicts

    fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            resolveConflictUseCase(conflict, useCloud)
        }
    }

    // =====================================================
    // ✏️ PROFILE UPDATE
    // =====================================================

    fun updateDisplayName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(name = newName.trim())
                updateUserUseCase(updatedUser)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal memperbarui nama")
            }
        }
    }

    // =====================================================
    // 🔄 MANTRA PENARIK NASIB (AUTO-REFRESH CLOUD)
    // =====================================================
    fun refreshCurrentUserFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = currentUser.value?.userId ?: return@launch
            syncUserUseCase(currentUserId)
        }
    }
}