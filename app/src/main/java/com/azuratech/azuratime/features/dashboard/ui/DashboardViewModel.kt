package com.azuratech.azuratime.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.domain.model.toAccountRole
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.auth.domain.repository.AuthRepository
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🏠 DASHBOARD VIEW MODEL (v3.2.0-ai-native)
 * Entry point for Accounts (Admins and Members).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: SchoolWorkspaceRepository,
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val biometricRepository: BiometricRepository,
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
) : ViewModel() {

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    private val _refreshTriggerFlow = MutableStateFlow(0)

    private val _accountFlow: StateFlow<AccountEntity?> = sessionManager.currentAccountIdFlow
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.observeAccountEntity(accountId).map { it.getOrNull() }
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeSchoolIdFlow = sessionManager.activeSchoolIdFlow

    private val _recentRecordsFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                attendanceRepository.getAttendanceRecords("", null, null, null, null, emptyList(), schoolId)
                    .map { result -> result.getOrNull()?.take(5) ?: emptyList() }
            } else {
                flowOf(emptyList())
            }
        }

    private val _allClassesFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                schoolRepository.observeClasses(schoolId).map { result ->
                    result.getOrNull() ?: emptyList()
                }
            } else {
                flowOf(emptyList())
            }
        }

    private val _assignedClassesFlow = combine(
        _activeSchoolIdFlow,
        _accountFlow,
    ) { schoolId, account ->
        schoolId to account
    }.flatMapLatest { (schoolId, account) ->
        if (schoolId != null && account != null) {
            schoolRepository.observeClasses(schoolId).map { result ->
                val allClasses = result.getOrNull() ?: emptyList()
                val membership = account.memberships[schoolId]
                if (membership?.role.toAccountRole() == AccountRole.ADMIN) {
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
                    .map { it.getOrNull() ?: emptyList() }
            } else {
                flowOf(emptyList())
            }
        }

    private val _activeSchoolFlow = _activeSchoolIdFlow
        .flatMapLatest { id ->
            if (id != null) {
                schoolRepository.observeSchoolById(id).map { result ->
                    result.getOrNull()
                }
            } else {
                flowOf<School?>(null)
            }
        }

    val uiStateFlow: StateFlow<DashboardUiState> = combine(
        _accountFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        _allClassesFlow,
        _activeSchoolFlow,
        _refreshTriggerFlow,
    ) { flows ->
        val account = flows[0] as AccountEntity?

        @Suppress("UNCHECKED_CAST")
        val recentRecords = flows[1] as List<AttendanceRecordEntity>

        @Suppress("UNCHECKED_CAST")
        val sessionStudents = flows[2] as List<StudentBiometricEntity>

        @Suppress("UNCHECKED_CAST")
        val assignedClasses = flows[3] as List<ClassModel>

        @Suppress("UNCHECKED_CAST")
        val allClasses = flows[4] as List<ClassModel>
        val activeSchool = flows[5] as School?

        val activeSchoolId = activeSchool?.id
        val membershipRole = if (account != null && activeSchoolId != null) account.memberships[activeSchoolId]?.role else null
        val effectiveRole = membershipRole ?: account?.role ?: "MEMBER"
        val isReady = account != null

        DashboardUiState(
            account = account,
            currentSchool = activeSchool,
            recentRecords = recentRecords,
            sessionStudents = sessionStudents,
            assignedClasses = assignedClasses,
            allClasses = allClasses,
            isReady = isReady,
            currentRole = effectiveRole,
            isApproved = account?.status == "ACTIVE",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    init {
        sessionManager.currentAccountIdFlow
            .filterNotNull()
            .onEach { triggerAutoSyncIfNeeded() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.LoadDashboard -> _refreshTriggerFlow.value++
            DashboardUiEvent.Refresh -> sync()
            is DashboardUiEvent.SelectSchool -> selectSchool(event.school)
            is DashboardUiEvent.SelectActiveClass -> selectActiveClass(event.classId)
            is DashboardUiEvent.ResolveConflict -> resolveConflict(event.conflict, event.useCloud)
            is DashboardUiEvent.NavigateTo -> viewModelScope.launch { _uiEventFlow.emit(UiEvent.NavigateTo(event.route)) }
            DashboardUiEvent.Logout -> logout()
            DashboardUiEvent.OnRegisterStudentClick -> onRegisterStudentClick()
        }
    }

    private suspend fun triggerAutoSyncIfNeeded() {
        val lastSync = sessionManager.getLastSyncTime()
        val isStale = System.currentTimeMillis() - lastSync > 24 * 60 * 60 * 1000
        if (isStale) sync()
    }

    private fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch
            accountRepository.syncAccount(accountId)
                .onSuccess { accountEntity ->
                    val schoolIds = accountEntity.memberships.keys.toList()

                    // 1. 🔥 FORCE SYNC school metadata
                    schoolRepository.syncSchools(schoolIds)

                    // 2. 🔥 Determine the active school ID
                    var activeSchoolId = accountEntity.activeSchoolId
                        ?: sessionManager.getActiveSchoolId()
                        ?: schoolIds.firstOrNull()

                    // 3. 🔥 Persist selection if we found one
                    if (!activeSchoolId.isNullOrBlank()) {
                        sessionManager.saveActiveSchoolId(activeSchoolId)

                        // 4. 🔥 Sync classes for ALL memberships to ensure they are available
                        schoolIds.forEach { id ->
                            schoolRepository.syncClasses(accountId, id)
                        }

                        // 5. 🔥 Sync core data for the active workspace
                        studentRepository.pullStudents(activeSchoolId)
                        biometricRepository.syncBiometrics()
                        studentRepository.autoHealStudentIdentities(activeSchoolId)
                        biometricRepository.syncAssignments()
                        attendanceRepository.syncRecords()

                        sessionManager.saveLastSyncTime(System.currentTimeMillis())
                    }

                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Sinkronisasi Selesai!"))
                }
                .onFailure { error ->
                    _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal sinkron: ${error.message}"))
                }
        }
    }

    private fun selectSchool(school: School) {
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentAccountId() ?: return@launch
            sessionManager.saveActiveSchoolId(school.id)
            workspaceRepository.switchWorkspace(accountId, school.id)
        }
    }

    private fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val account = uiStateFlow.value.account ?: return@launch
            database.accountDao().updateAccount(account.copy(activeClassId = classId))
        }
    }

    private fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            attendanceRepository.resolveConflict(conflict.conflictId, useCloud)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.clearAllDataAndSignOut()
            _uiEventFlow.emit(UiEvent.NavigateTo("login")) // Assuming login route
        }
    }

    private fun onRegisterStudentClick() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Silakan pilih sekolah terlebih dahulu"))
                return@launch
            }
            _uiEventFlow.emit(UiEvent.NavigateTo("registrationMenu"))
        }
    }
}
