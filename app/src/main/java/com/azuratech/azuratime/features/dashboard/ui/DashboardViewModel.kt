package com.azuratech.azuratime.features.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
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
import com.azuratech.azuratime.core.util.isAdmin
import com.azuratech.azuratime.features.account.data.local.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🏠 DASHBOARD VIEW MODEL (v3.2.0-ai-native)
 * Optimized with Effect-Driven MVI pattern.
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

    private val _uiEffectFlow = MutableSharedFlow<DashboardUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _refreshTriggerFlow = MutableStateFlow(0)

    private val _accountFlow: StateFlow<AccountEntity?> = sessionManager.currentAccountIdFlow
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.observeAccountEntityFlow(accountId).map { it.getOrNull() }
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeSchoolIdFlow = sessionManager.activeSchoolIdFlow

    private val _recentRecordsFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                attendanceRepository.getAttendanceRecordsFlow("", null, null, null, null, emptyList(), schoolId)
                    .map { result -> result.getOrNull()?.take(5) ?: emptyList() }
            } else {
                flowOf(emptyList())
            }
        }

    private val _allClassesFlow = combine(
        _activeSchoolIdFlow,
        _accountFlow,
        studentRepository.getStudentProfilesFlow(),
    ) { schoolId, account, studentResult ->
        Triple(schoolId, account, studentResult)
    }.flatMapLatest { (schoolId, account, studentResult) ->
        if (schoolId != null && account != null) {
            schoolRepository.observeClassesFlow(schoolId).map { result ->
                val classes = result.getOrNull() ?: emptyList()
                val students = studentResult.getOrNull() ?: emptyList()

                // 🔥 AI Native: Map dynamic counts from SSOT assignments
                val enrichedClasses = classes.map { classModel ->
                    val count = students.count { it.classIds.contains(classModel.id) }
                    classModel.copy(studentCount = count)
                }

                if (account.toDomain().isAdmin(schoolId)) {
                    enrichedClasses
                } else {
                    val membership = account.memberships[schoolId]
                    val assignedIds = membership?.assignedClassIds ?: emptyList()
                    enrichedClasses.filter { it.id in assignedIds }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }

    private val _assignedClassesFlow = _allClassesFlow

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
                schoolRepository.observeSchoolByIdFlow(id).map { result ->
                    result.getOrNull()
                }
            } else {
                flowOf<School?>(null)
            }
        }

    private val _pendingRequestsCountFlow = sessionManager.currentAccountIdFlow
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.observePendingRequestsCountFlow(accountId)
            } else {
                flowOf(0)
            }
        }

    private val _totalActiveStudentsFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                studentRepository.getStudentProfilesFlow()
                    .map { result ->
                        val students = result.getOrNull() ?: emptyList()
                        students.count { it.classIds.isNotEmpty() }
                    }
            } else {
                flowOf(0)
            }
        }

    private val _geofenceFlow = _activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                schoolRepository.observeGeofenceFlow(schoolId)
            } else {
                flowOf(null)
            }
        }

    val uiStateFlow: StateFlow<DashboardUiState> = combine(
        _accountFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        _allClassesFlow,
        _activeSchoolFlow,
        _pendingRequestsCountFlow,
        _totalActiveStudentsFlow,
        _geofenceFlow,
        sessionManager.isLoggingOutFlow,
        _refreshTriggerFlow,
    ) { params ->
        val account = params[0] as AccountEntity?

        @Suppress("UNCHECKED_CAST")
        val recentRecords = params[1] as List<AttendanceRecordEntity>

        @Suppress("UNCHECKED_CAST")
        val sessionStudents = params[2] as List<StudentBiometricEntity>

        @Suppress("UNCHECKED_CAST")
        val assignedClasses = params[3] as List<ClassModel>

        @Suppress("UNCHECKED_CAST")
        val allClasses = params[4] as List<ClassModel>

        println("🛠 DEBUG DASHBOARD COMBINE: AssignedCount=${assignedClasses.size}, AllCount=${allClasses.size}")
        val activeSchool = params[5] as School?
        val pendingCount = params[6] as Int
        val totalActiveStudents = params[7] as Int
        val geofence = params[8] as com.azuratech.azuratime.features.school.data.local.GpsGeofenceEntity?
        val isLoggingOut = params[9] as Boolean

        val activeSchoolId = activeSchool?.id
        val membershipRole = if (account != null && activeSchoolId != null) account.memberships[activeSchoolId]?.role else null
        val effectiveRole = membershipRole ?: account?.role ?: "USER"
        val isReady = account != null

        val needsAssignment = effectiveRole == "SUPERVISOR" && assignedClasses.isEmpty()

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
            needsClassAssignment = needsAssignment,
            pendingRequests = pendingCount,
            totalActiveStudents = totalActiveStudents,
            geofence = geofence,
            isLoggingOut = isLoggingOut,
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
            DashboardUiEvent.LoadDashboard -> {
                _refreshTriggerFlow.value++
            }
            DashboardUiEvent.Refresh -> sync()
            is DashboardUiEvent.SelectSchool -> selectSchool(event.school)
            is DashboardUiEvent.SelectActiveClass -> selectActiveClass(event.classId)
            is DashboardUiEvent.ResolveConflict -> resolveConflict(event.conflict, event.useCloud)
            is DashboardUiEvent.NavigateTo -> viewModelScope.launch { _uiEffectFlow.emit(DashboardUiEffect.NavigateTo(event.route)) }
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

                    _uiEffectFlow.emit(DashboardUiEffect.ShowSnackbar("Sync Completed!"))
                }
                .onFailure { error ->
                    _uiEffectFlow.emit(DashboardUiEffect.ShowSnackbar("Sync Failed: ${error.message}"))
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

    private fun selectActiveClass(classId: String?, navigateTo: String? = null) {
        viewModelScope.launch {
            val account = uiStateFlow.value.account ?: return@launch
            val updated = account.copy(activeClassId = classId)
            database.accountDao().updateAccount(updated)

            // 🔥 AI Native: Push session change immediately to prevent sync overwrite
            accountRepository.pushAccount(account.accountId)

            if (navigateTo != null) {
                _uiEffectFlow.emit(DashboardUiEffect.NavigateTo(navigateTo))
            }
        }
    }

    private fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            attendanceRepository.resolveConflict(conflict.conflictId, useCloud)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _refreshTriggerFlow.update { -1 } // Optional: stop other combine flows
            // We can't update uiStateFlow directly as it is a combine(...) stateIn
            // But we can add it to the combine if we want, or just rely on the effect.
            // Actually, let's just make it emit the effect and let the UI handle it.
            // Wait, the UI needs to show the overlay.
            // Since uiStateFlow is a combine of many things, adding one more StateFlow is best.
            authRepository.clearAllDataAndSignOut()
        }
    }

    private fun onRegisterStudentClick() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId == null) {
                _uiEffectFlow.emit(DashboardUiEffect.ShowSnackbar("Please select a school first"))
                return@launch
            }
            _uiEffectFlow.emit(DashboardUiEffect.NavigateTo("registrationMenu"))
        }
    }
}
