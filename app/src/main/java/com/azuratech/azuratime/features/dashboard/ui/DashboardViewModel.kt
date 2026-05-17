package com.azuratech.azuratime.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.account.data.repo.AdminRepository
import com.azuratech.azuratime.features.account.data.repo.SchoolWorkspaceRepository
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceConflict
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.auth.data.repo.AuthRepository
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val accountRepository: AccountRepository,
    private val biometricRepository: StudentBiometricRepository,
    private val studentRepository: com.azuratech.azuratime.features.student.domain.repository.StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: SchoolWorkspaceRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _accountFlow = sessionManager.currentUserIdFlow
        .flatMapLatest { accountId ->
            if (accountId != null) accountRepository.observeAccountEntity(accountId) else flowOf(null)
        }

    private val _activeSchoolIdFlow = sessionManager.activeSchoolIdFlow

    private val _recentRecordsFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                attendanceRepository.getAttendanceRecords("", null, null, null, null, emptyList(), schoolId).map { it.take(5) }
            } else {
                flowOf(emptyList())
            }
        }

    private val _allClassesFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                schoolRepository.observeClasses(schoolId).map { result ->
                    if (result is Result.Success) result.data else emptyList()
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

    private val _activeSchoolFlow = _activeSchoolIdFlow
        .flatMapLatest { id ->
            if (id != null) schoolRepository.observeSchoolById(id) else flowOf<School?>(null)
        }

    val uiState: StateFlow<DashboardUiState> = combine(
        _accountFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        _allClassesFlow,
        _activeSchoolFlow,
        _refreshTrigger,
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
        val membershipRole = account?.memberships?.get(activeSchoolId)?.role
        val effectiveRole = membershipRole ?: account?.role ?: "USER"
        val isReady = account != null

        DashboardUiState(
            user = account,
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
        sessionManager.currentUserIdFlow
            .filterNotNull()
            .onEach { triggerAutoSyncIfNeeded() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            DashboardUiEvent.LoadDashboard -> _refreshTrigger.value++
            DashboardUiEvent.Refresh -> sync()
            is DashboardUiEvent.SelectSchool -> selectSchool(event.school)
            is DashboardUiEvent.SelectActiveClass -> selectActiveClass(event.classId)
            is DashboardUiEvent.ResolveConflict -> resolveConflict(event.conflict, event.useCloud)
            is DashboardUiEvent.NavigateTo -> viewModelScope.launch { _uiEvent.emit(UiEvent.NavigateTo(event.route)) }
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
            val accountId = sessionManager.getCurrentUserId() ?: return@launch
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

                    _uiEvent.emit(UiEvent.ShowSnackbar("Sinkronisasi Selesai!"))
                }
                .onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal sinkron: ${error.message}"))
                }
        }
    }

    private fun selectSchool(school: School) {
        viewModelScope.launch {
            val accountId = sessionManager.getCurrentUserId() ?: return@launch
            sessionManager.saveActiveSchoolId(school.id)
            workspaceRepository.switchWorkspace(accountId, school.id)
        }
    }

    private fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val account = uiState.value.user ?: return@launch
            accountRepository.getAccountDao().updateAccount(account.copy(activeClassId = classId))
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
            _uiEvent.emit(UiEvent.NavigateTo("login")) // Assuming login route
        }
    }

    private fun onRegisterStudentClick() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Silakan pilih sekolah terlebih dahulu"))
                return@launch
            }
            _uiEvent.emit(UiEvent.NavigateTo("registration_menu"))
        }
    }
}
