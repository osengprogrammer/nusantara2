package com.azuratech.azuratime.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.repo.AdminRepository
import com.azuratech.azuratime.features.auth.data.repo.AuthRepository
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.ui.UiEvent
import kotlinx.coroutines.channels.Channel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val accountRepository: AccountRepository,
    private val biometricRepository: StudentBiometricRepository,
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: com.azuratech.azuratime.features.school.data.repo.SchoolRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class NavigationEvent {
        data class NavigateToRegistration(val schoolId: String) : NavigationEvent()
    }

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _accountFlow = sessionManager.currentUserIdFlow
        .flatMapLatest { accountId -> 
            if (accountId != null) {
                accountRepository.observeAccountEntity(accountId)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _recentRecordsFlow = sessionManager.activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                attendanceRepository.getAttendanceRecords("", null, null, null, null, emptyList(), schoolId).map { it.take(5) }
            } else {
                flowOf(emptyList())
            }
        }

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { result ->
                if (result is Result.Success) result.data else emptyList()
            }
        }

    private val _assignedClassesFlow = combine(
        sessionManager.activeSchoolIdFlow,
        _accountFlow
    ) { schoolId, account ->
        schoolId to account
    }.flatMapLatest { (schoolId, account) ->
        if (schoolId != null && account != null) {
            schoolRepository.observeClasses(schoolId).map { result ->
                val allClasses = if (result is Result.Success) result.data else emptyList()
                val membership = account.memberships[schoolId]
                if (membership?.role == "ADMIN") {
                    allClasses
                } else {
                    val assignedIds = membership?.assignedClassIds ?: emptyList()
                    allClasses.filter { it.id in assignedIds }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }

    private val _sessionStudentsFlow = _accountFlow
        .flatMapLatest { account ->
            val activeClassId = account?.activeClassId
            val schoolId = account?.activeSchoolId
            if (activeClassId != null && schoolId != null) {
                biometricRepository.getStudentsInClassFlow(activeClassId, schoolId)
            } else {
                flowOf(emptyList())
            }
        }


    init {
        // 🔥 FIX: React to ID changes instead of one-shot check
        sessionManager.currentUserIdFlow
            .filterNotNull()
            .onEach { accountId -> 
                println("🔍 Dashboard: ID detected ($accountId), triggering sync...")
                triggerAutoSyncIfNeeded() 
            }
            .launchIn(viewModelScope)
    }

    private suspend fun triggerAutoSyncIfNeeded() {
        val lastSync = sessionManager.getLastSyncTime()
        val isStale = System.currentTimeMillis() - lastSync > 24 * 60 * 60 * 1000 // 24h stale logic
        
        if (isStale) {
            println("🔄 Persistence: Auto-sync triggered (isStale=$isStale).")
            sync()
        }
    }

    val state: StateFlow<UiState<DashboardUiState>> = combine(
        _accountFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        _allClassesFlow,
        flowOf(false), // syncRepository removed
        flowOf(0), // dataIntegrity removed
        flowOf(0),
        flowOf(0),
        flowOf(0),
        flowOf(emptyList<AttendanceConflict>())
    ) { args ->
        val account = args[0] as AccountEntity?
        @Suppress("UNCHECKED_CAST")
        val recentRecords = args[1] as List<AttendanceRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val sessionStudents = args[2] as List<StudentBiometricEntity>
        @Suppress("UNCHECKED_CAST")
        val assignedClasses = args[3] as List<ClassModel>
        @Suppress("UNCHECKED_CAST")
        val allClasses = args[4] as List<ClassModel>
        val isSyncing = args[5] as Boolean
        val totalStudents = args[6] as Int
        val unassigned = args[7] as Int
        val broken = args[8] as Int
        val unsynced = args[9] as Int
        @Suppress("UNCHECKED_CAST")
        val conflicts = args[10] as List<AttendanceConflict>

        val isReady = (account != null) && (isSyncing == false)

        println("🔄 Dashboard combine: account=${account?.accountId ?: "NULL"}, isReady=$isReady")

        val activeSchoolId = sessionManager.getActiveSchoolId()
        val membershipRole = account?.memberships?.get(activeSchoolId)?.role
        val effectiveRole = membershipRole ?: account?.role ?: "USER"

        UiState.Success(
            DashboardUiState(
                user = account,
                recentRecords = recentRecords,
                sessionStudents = sessionStudents,
                assignedClasses = assignedClasses,
                allClasses = allClasses,
                isSyncing = isSyncing,
                isReady = isReady,
                totalStudents = totalStudents,
                unassignedStudents = unassigned,
                brokenAssignments = broken,
                unsyncedRecords = unsynced,
                conflicts = conflicts,
                currentRole = effectiveRole,
                isApproved = account?.status == "ACTIVE"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            val accountId = sessionManager.getCurrentUserId() ?: return@launch
            
            println("🔄 Dashboard: Comprehensive sync starting for account $accountId...")
            
            // 1. Restore Profile & Schools
            val accountResult = accountRepository.syncAccount(accountId)
            val accountEntity = if (accountResult is Result.Success) accountResult.data else accountRepository.getAccountDao().getAccountById(accountId)
            
            // 2. Restore Classes for all member schools
            accountEntity?.memberships?.keys?.forEach { schoolId ->
                println("🏫 Dashboard: Syncing classes for school: $schoolId")
                schoolRepository.syncClasses(accountId, schoolId)
            }

            // 3. Restoring biometrics & assignments (tenant-scoped)
            val schoolId = accountEntity?.activeSchoolId ?: sessionManager.getActiveSchoolId()
            
            if (schoolId != null) {
                println("🎭 Dashboard: Syncing school-specific data for: $schoolId")
                
                if (sessionManager.getActiveSchoolId().isNullOrBlank()) {
                    sessionManager.saveActiveSchoolId(schoolId)
                }

                val biometricSyncResult = biometricRepository.syncBiometrics()
                biometricRepository.syncAssignments()
                attendanceRepository.syncRecords()

                if (biometricSyncResult is Result.Failure) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal sinkron data biometrik: ${biometricSyncResult.error.message}"))
                }
            }
            
            sessionManager.saveLastSyncTime(System.currentTimeMillis())
            _uiEvent.emit(UiEvent.ShowSnackbar("Sinkronisasi Selesai!"))
            println("✅ Dashboard: Comprehensive sync completed.")
        }
    }

    fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val currentState = state.value
            if (currentState is UiState.Success) {
                val account = currentState.data.user ?: return@launch
                accountRepository.getAccountDao().getAccountById(account.accountId)?.let {
                    accountRepository.getAccountDao().updateAccount(it.copy(activeClassId = classId))
                }
            }
        }
    }

    fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            attendanceRepository.resolveConflict(conflict.conflictId, useCloud)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.clearAllDataAndSignOut()
            onComplete()
        }
    }

    fun onRegisterStudentClick() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Silakan pilih sekolah terlebih dahulu"))
                return@launch
            }
            _navigationEvent.emit(NavigationEvent.NavigateToRegistration(schoolId))
        }
    }
}
