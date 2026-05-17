package com.azuratech.azuratime.features.account.ui.management

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * 🛠️ ACCOUNT MANAGEMENT VIEW MODEL
 * Pengelola profil akun, hak akses kelas, dan relasi antar pengajar.
 * 🔥 Refactored: Fully SSOT! Observing AccountEntity directly.
 */
@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase,
    private val repository: AccountRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    // =====================================================
    // 1. DIRI SENDIRI (Active Admin/Account Session)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<AccountEntity?> = sessionManager.currentUserIdStateFlow
        .filterNotNull()
        .flatMapLatest { uid -> repository.observeAccountEntity(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val assignedClassIds: StateFlow<List<String>> = sessionManager.activeSchoolIdStateFlow
        .filterNotNull()
        .combine(sessionManager.currentUserIdStateFlow.filterNotNull()) { schoolId, accountId -> schoolId to accountId }
        .flatMapLatest { (schoolId, accountId) -> database.accountClassAccessDao().getAssignedClassIds(accountId, schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 2. JARINGAN (Explore Accounts in the same school)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUsersInSameSchool: StateFlow<List<AccountEntity>> = sessionManager.activeSchoolIdStateFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            repository.getAccountDao().observeAllAccounts().map { accounts ->
                accounts.filter { it.memberships.containsKey(schoolId) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 3. TARGET MANAGEMENT (Managing other accounts)
    // =====================================================

    private val _selectedTargetUserFlow = MutableStateFlow<AccountEntity?>(null)
    val selectedTargetUserFlow: StateFlow<AccountEntity?> = _selectedTargetUserFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val targetAssignedClassIds: StateFlow<List<String>> = _selectedTargetUserFlow
        .filterNotNull()
        .combine(sessionManager.activeSchoolIdStateFlow.filterNotNull()) { target, schoolId -> target.accountId to schoolId }
        .flatMapLatest { (targetId, schoolId) -> database.accountClassAccessDao().getAssignedClassIds(targetId, schoolId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTargetUser(accountId: String, name: String, email: String) {
        _selectedTargetUserFlow.value = AccountEntity(
            accountId = accountId,
            name = name,
            email = email
        )
    }

    // =====================================================
    // 🛠️ OPERATIONS
    // =====================================================

    fun selectActiveClass(classId: String?, targetAccountId: String? = null) {
        val accountId = targetAccountId ?: currentUser.value?.accountId ?: return
        
        viewModelScope.launch {
            val accountToUpdate: AccountEntity? = if (targetAccountId == null || targetAccountId == currentUser.value?.accountId) {
                currentUser.value
            } else {
                repository.getAccountDao().getAccountById(targetAccountId)
            }

            accountToUpdate?.let {
                val updatedAccount = it.copy(activeClassId = classId)
                repository.getAccountDao().updateAccount(updatedAccount)
                repository.pushAccount(accountId)
            }
        }
    }

    fun assignClassToUser(classId: String, targetAccountId: String? = null) {
        // Placeholder for class assignment logic
    }

    fun removeClassAccess(@Suppress("UNUSED_PARAMETER") classId: String, targetAccountId: String? = null) {
        // Placeholder for class access removal
    }

    // =====================================================
    // ✏️ PROFILE UPDATE
    // =====================================================

    fun updateDisplayName(newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val account = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val updatedAccount = account.copy(name = newName.trim())
                repository.getAccountDao().updateAccount(updatedAccount)
                repository.pushAccount(account.accountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal memperbarui nama")
            }
        }
    }

    // =====================================================
    // 🔄 REFRESH CLOUD
    // =====================================================
    fun refreshCurrentUserFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccountId = currentUser.value?.accountId ?: return@launch
            repository.syncAccount(currentAccountId)
        }
    }
}
