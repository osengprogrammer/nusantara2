package com.azuratech.azuratime.features.staff.ui.management

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.staff.data.local.StaffAccountEntity
import com.azuratech.azuratime.features.staff.data.local.toEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ USER MANAGEMENT VIEW MODEL
 * Pengelola profil user, hak akses kelas, dan relasi antar pengajar.
 * 🔥 Refactored: Fully SSOT! Observing StaffAccountEntity directly.
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase,
    private val repository: StaffAccountRepository,
    private val checkInRepository: AttendanceRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    // =====================================================
    // 1. DIRI SENDIRI (Active Admin/Teacher Session)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<StaffAccountEntity?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid -> repository.getUserDao().observeUserById(uid) }
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
    val allUsersInSameSchool: StateFlow<List<StaffAccountEntity>> = sessionManager.activeSchoolIdFlow
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

    private val _selectedTargetUser = MutableStateFlow<StaffAccountEntity?>(null)
    val selectedTargetUser: StateFlow<StaffAccountEntity?> = _selectedTargetUser.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val targetAssignedClassIds: StateFlow<List<String>> = _selectedTargetUser
        .filterNotNull()
        .combine(sessionManager.activeSchoolIdFlow.filterNotNull()) { target, schoolId -> target.userId to schoolId }
        .flatMapLatest { (targetId, schoolId) -> repository.getUserClassAccessDao().observeClassIdsForUser(targetId, schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTargetUser(userId: String, name: String, email: String) {
        _selectedTargetUser.value = StaffAccountEntity(
            userId = userId,
            name = name,
            email = email
        )
    }

    // =====================================================
    // 🛠️ OPERATIONS
    // =====================================================

    fun selectActiveClass(classId: String?, targetUserId: String? = null) {
        val userId = targetUserId ?: currentUser.value?.userId ?: return
        println("🖱 DEBUG: selectActiveClass called for userId=$userId, classId=$classId")
        
        viewModelScope.launch {
            val userToUpdate: StaffAccountEntity? = if (targetUserId == null || targetUserId == currentUser.value?.userId) {
                currentUser.value
            } else {
                repository.getUserDao().getUserById(targetUserId)
            }

            userToUpdate?.let {
                val updatedUser = it.copy(activeClassId = classId)
                println("💾 DEBUG: Saving user with activeClassId=${updatedUser.activeClassId}")
                val result = repository.updateUser(updatedUser.toDomain())
                if (result is com.azuratech.azuraengine.result.Result.Success<Unit>) {
                    println("✅ DEBUG: selectActiveClass success for classId=$classId")
                } else if (result is com.azuratech.azuraengine.result.Result.Failure) {
                    println("❌ DEBUG: selectActiveClass failed: ${result.error}")
                }
            }
        }
    }

    fun assignClassToUser(classId: String, targetUserId: String? = null) {
        val targetId = targetUserId ?: currentUser.value?.userId ?: return
        val schoolId = currentUser.value?.activeSchoolId ?: return
        viewModelScope.launch {
            repository.approveMembership(targetId, schoolId, "", "TEACHER", listOf(classId))
        }
    }

    fun removeClassAccess(classId: String, targetUserId: String? = null) {
        val targetId = targetUserId ?: currentUser.value?.userId ?: return
        val schoolId = currentUser.value?.activeSchoolId ?: return
        viewModelScope.launch {
            repository.revokeMembership(targetId, schoolId)
        }
    }

    // =====================================================
    // 🚪 CONFLICT RESOLUTION
    // =====================================================
    val conflicts = repository.conflicts

    fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            checkInRepository.resolveConflict(conflict.conflictId, useCloud)
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
                repository.updateUser(updatedUser)
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
            repository.syncUser(currentUserId)
        }
    }
}