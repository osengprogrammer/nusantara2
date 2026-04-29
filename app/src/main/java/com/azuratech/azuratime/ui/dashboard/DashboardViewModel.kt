package com.azuratech.azuratime.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repo.AdminRepository
import com.azuratech.azuratime.data.repo.AuthRepository
import com.azuratech.azuratime.data.repo.DataIntegrityRepository
import com.azuratech.azuratime.domain.face.usecase.GetFacesInClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.checkin.usecase.GetCheckInRecordsUseCase
import com.azuratech.azuratime.domain.checkin.usecase.CheckInFilters
import com.azuratech.azuratime.domain.checkin.usecase.SyncCheckInRecordsUseCase
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.domain.user.usecase.ObserveUserUseCase
import com.azuratech.azuratime.domain.user.usecase.UpdateUserUseCase
import com.azuratech.azuratime.domain.checkin.usecase.ResolveConflictUseCase
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import kotlinx.coroutines.channels.Channel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AttendanceConflict
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
    private val database: AppDatabase,
    private val adminRepository: AdminRepository,
    private val observeUserUseCase: ObserveUserUseCase,
    private val syncUserUseCase: SyncUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getClassesUseCase: GetClassesUseCase,
    private val resolveConflictUseCase: ResolveConflictUseCase,
    private val getCheckInRecordsUseCase: GetCheckInRecordsUseCase,
    private val syncCheckInRecordsUseCase: SyncCheckInRecordsUseCase,
    private val authRepository: AuthRepository,
    private val dataIntegrityRepository: DataIntegrityRepository,
    private val getFacesInClassUseCase: GetFacesInClassUseCase,
    private val sessionManager: SessionManager,
    private val syncViewModel: SyncViewModel
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _userFlow = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { userId -> observeUserUseCase(userId) }

    private val _recentRecordsFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            val filters = CheckInFilters()
            getCheckInRecordsUseCase(filters).map { it.getOrNull()?.take(5) ?: emptyList() }
        }

    private val _assignedClassesFlow = combine(
        sessionManager.activeSchoolIdFlow.filterNotNull(),
        _userFlow.filterNotNull()
    ) { schoolId, user ->
        schoolId to user
    }.flatMapLatest { (schoolId, user) ->
        getClassesUseCase(schoolId).map { result ->
            val allClasses = if (result is Result.Success) result.data else emptyList()
            val membership = user.memberships[schoolId]
            if (membership?.role == "ADMIN") {
                allClasses
            } else {
                val assignedIds = membership?.assignedClassIds ?: emptyList()
                allClasses.filter { it.id in assignedIds }
            }
        }
    }

    private val _sessionStudentsFlow = _userFlow
        .filterNotNull()
        .flatMapLatest { user ->
            val activeClassId = user.activeClassId
            if (activeClassId != null) {
                getFacesInClassUseCase(activeClassId)
                    .map { it.getOrNull() ?: emptyList() }
            } else {
                flowOf(emptyList())
            }
        }


    init {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId()
            if (userId != null) {
                syncUserUseCase(userId)
            }
        }
    }

    val state: StateFlow<UiState<DashboardUiState>> = combine(
        _userFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        syncViewModel.isSyncing,
        dataIntegrityRepository.totalFaces,
        dataIntegrityRepository.missingAssignment,
        dataIntegrityRepository.brokenAssignments,
        dataIntegrityRepository.globalUnsyncedCount,
        dataIntegrityRepository.conflicts
    ) { args ->
        val user = args[0] as com.azuratech.azuraengine.model.User?
        @Suppress("UNCHECKED_CAST")
        val recentRecords = args[1] as List<com.azuratech.azuratime.data.local.CheckInRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val sessionStudents = args[2] as List<FaceEntity>
        @Suppress("UNCHECKED_CAST")
        val assignedClasses = args[3] as List<ClassModel>
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

        // 🔑 Priority: Global role > School role > Account status
        val globalRole = user.role  // "SUPER_ADMIN", "ADMIN", "USER", etc.
        val currentWorkspaceId = user.activeSchoolId
        val membershipRole = currentWorkspaceId?.let { user.memberships[it]?.role }
        
        val currentRole = when {
            globalRole == "SUPER_ADMIN" -> "SUPER_ADMIN"
            membershipRole != null -> membershipRole
            else -> if (user.status != "PENDING") user.status else "USER"
        }
        println("🔍 DEBUG: globalRole=${user.role}, membershipRole=$membershipRole, currentRole=$currentRole")

        val isApproved = currentRole == "ADMIN" || currentRole == "TEACHER" || currentRole == "SUPER_ADMIN"

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
    }
.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun sync() {
        viewModelScope.launch {
            val userId = sessionManager.getCurrentUserId()
            if (userId != null) {
                syncUserUseCase(userId)
            }
        }
        syncViewModel.forceSyncFromCloud {
            viewModelScope.launch { 
                syncCheckInRecordsUseCase()
                _uiEvent.emit(UiEvent.ShowSnackbar("Sinkronisasi Selesai!")) 
            }
        }
    }

    fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val currentState = state.value
            if (currentState is UiState.Success) {
                val user = currentState.data.user ?: return@launch
                
                // 🔥 CRITICAL: Fetch UserEntity from DB for write operation
                val userEntity = database.userDao().getUserById(user.userId)
                
                userEntity?.let {
                    val updatedUser = it.copy(activeClassId = classId)
                    println("✅ VM: Saved activeClassId=$classId for user ${user.userId}")
                    updateUserUseCase(updatedUser)
                }
            }
        }
    }

    fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            resolveConflictUseCase(conflict, useCloud)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.clearAllDataAndSignOut()
            onComplete()
        }
    }
}
