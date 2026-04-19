package com.azuratech.azuratime.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repository.AuthRepository
import com.azuratech.azuratime.data.repository.CheckInRepository
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.data.repository.DataIntegrityRepository
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.repository.UserRepository
import com.azuratech.azuratime.data.repository.AdminRepository
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.channels.Channel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.ui.sync.SyncViewModel
import com.azuratech.azuratime.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
    private val classRepository: ClassRepository,
    private val checkInRepository: CheckInRepository,
    private val authRepository: AuthRepository,
    private val dataIntegrityRepository: DataIntegrityRepository,
    private val faceRepository: FaceRepository,
    private val sessionManager: SessionManager,
    private val syncViewModel: SyncViewModel
) : ViewModel() {

    private val _syncCompletedEvent = Channel<Unit>()
    val syncCompletedEvent = _syncCompletedEvent.receiveAsFlow()

    private val _userFlow = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { userId -> userRepository.observeUserById(userId) }

    private val _assignedClassesFlow = _userFlow
        .filterNotNull()
        .flatMapLatest { user ->
            val membershipRole = user.activeSchoolId?.let { user.memberships[it]?.role }
            val role = membershipRole ?: (if (user.status != "PENDING") user.status else "USER")
            
            if (role == "ADMIN" || role == "SUPER_USER") {
                classRepository.allClasses.onEach {
                    android.util.Log.d("AZURA_SYNC", "Assigned classes emitted (ADMIN): ${it.size}")
                }
            } else {
                userRepository.observeClassIdsForUser(user.userId).combine(classRepository.allClasses) { assignedIds, allClasses ->
                    val filtered = allClasses.filter { it.id in assignedIds }
                    android.util.Log.d("AZURA_SYNC", "Assigned classes emitted (TEACHER): ${filtered.size}")
                    filtered
                }
            }
        }

    private val _recentRecordsFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            checkInRepository.getFilteredRecords(
                nameFilter = "", startDate = null, endDate = null,
                userId = null, classId = null, assignedIds = emptyList(),
                schoolId = schoolId
            ).map { it.getOrNull()?.take(5) ?: emptyList() }
        }

    private val _sessionStudentsFlow = _userFlow
        .filterNotNull()
        .flatMapLatest { user ->
            val activeClassId = user.activeClassId
            if (activeClassId != null) {
                faceRepository.getFacesInClassFlow(activeClassId)
                    .map { it.getOrNull() ?: emptyList() }
            } else {
                flowOf(emptyList())
            }
        }


    init {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId()
            if (userId != null) {
                userRepository.refreshUserFromCloud(userId)
            }
        }
        viewModelScope.launch {
            _userFlow.collectLatest { user ->
                if (user != null) {
                    checkInRepository.setActiveClass(user.activeClassId)
                    val role = user.activeSchoolId?.let { user.memberships[it]?.role }
                    if (role == "ADMIN" && user.activeSchoolId != null) {
                        adminRepository.startObservingTeachers(user.activeSchoolId)
                    }
                }
            }
        }
    }

    val state: StateFlow<UiState<DashboardUiState>> = combine(
        _userFlow,
        _assignedClassesFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        syncViewModel.isSyncing,
        dataIntegrityRepository.totalFaces,
        dataIntegrityRepository.missingAssignment,
        dataIntegrityRepository.brokenAssignments,
        dataIntegrityRepository.globalUnsyncedCount,
        userRepository.conflicts
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val user = args[0] as com.azuratech.azuratime.data.local.UserEntity?
        @Suppress("UNCHECKED_CAST")
        val assignedClasses = args[1] as List<com.azuratech.azuratime.data.local.ClassEntity>
        @Suppress("UNCHECKED_CAST")
        val recentRecords = args[2] as List<com.azuratech.azuratime.data.local.CheckInRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val sessionStudents = args[3] as List<FaceEntity>
        val isSyncing = args[4] as Boolean
        val totalFaces = args[5] as Int
        val unassigned = args[6] as Int
        val broken = args[7] as Int
        val unsynced = args[8] as Int
        @Suppress("UNCHECKED_CAST")
        val conflicts = args[9] as List<com.azuratech.azuratime.data.local.AttendanceConflict>

        if (user == null) {
            return@combine UiState.Loading
        }

        val currentWorkspaceId = user.activeSchoolId
        val membershipRole = currentWorkspaceId?.let { user.memberships[it]?.role }
        val topLevelStatus = user.status
        
        // Prioritize membership role if it exists, otherwise fallback to top-level status or USER
        val currentRole = membershipRole ?: (if (topLevelStatus != "PENDING") topLevelStatus else "USER")
        val isApproved = currentRole == "ADMIN" || currentRole == "TEACHER"
        
        android.util.Log.d("AZURA_DEBUG", "Membership Role: $membershipRole | Top Level Status: $topLevelStatus | Final Current Role: $currentRole")
        android.util.Log.d("AZURA_ROLE", "Workspace: $currentWorkspaceId | Role: $currentRole | isApproved: $isApproved")
        
        val pendingRequests = user.friends.values.count { it.status == "PENDING_APPROVAL" }

        val dashboardState = DashboardUiState(
            user = user,
            assignedClasses = assignedClasses,
            recentRecords = recentRecords,
            sessionStudents = sessionStudents,
            isSyncing = isSyncing,
            pendingRequests = pendingRequests,
            currentRole = currentRole,
            isApproved = isApproved,
            totalFaces = totalFaces,
            unassignedStudents = unassigned,
            brokenAssignments = broken,
            unsyncedRecords = unsynced,
            conflicts = conflicts
        )
        UiState.Success(dashboardState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val user = _userFlow.first()
            if (user != null) {
                userRepository.updateActiveClass(user, classId)
            }
        }
    }

    fun resolveConflict(conflict: com.azuratech.azuratime.data.local.AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            userRepository.resolveAttendanceConflict(conflict, useCloud)
        }
    }

    fun sync() {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId()
            if (userId != null) {
                userRepository.refreshUserFromCloud(userId)
            }
        }
        syncViewModel.forceSyncFromCloud {
            viewModelScope.launch { _syncCompletedEvent.send(Unit) }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.clearAllDataAndSignOut()
            onComplete()
        }
    }
}