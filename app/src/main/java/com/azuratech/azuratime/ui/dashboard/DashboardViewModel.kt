package com.azuratech.azuratime.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.repo.AdminRepository
import com.azuratech.azuratime.data.repo.AuthRepository
import com.azuratech.azuratime.data.repo.DataIntegrityRepository
import com.azuratech.azuratime.data.repo.SyncRepository
import com.azuratech.azuratime.domain.face.usecase.GetFacesInClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.checkin.usecase.GetCheckInRecordsUseCase
import com.azuratech.azuratime.domain.checkin.usecase.CheckInFilters
import com.azuratech.azuratime.domain.checkin.usecase.SyncCheckInRecordsUseCase
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import com.azuratech.azuratime.domain.user.usecase.ObserveUserUseCase
import com.azuratech.azuratime.domain.user.usecase.UpdateUserUseCase
import com.azuratech.azuratime.domain.user.usecase.GetUserByIdUseCase
import com.azuratech.azuratime.domain.school.usecase.SyncSchoolsUseCase
import com.azuratech.azuratime.domain.face.usecase.SyncFacesUseCase
import com.azuratech.azuratime.domain.assignment.usecase.SyncAssignmentsUseCase
import com.azuratech.azuratime.domain.sync.usecase.GetLocalDataCountUseCase
import com.azuratech.azuratime.domain.checkin.usecase.ResolveConflictUseCase
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import kotlinx.coroutines.channels.Channel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AttendanceConflict
import com.azuratech.azuratime.ui.util.UiState
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
    private val observeUserUseCase: ObserveUserUseCase,
    private val syncUserUseCase: SyncUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getClassesUseCase: GetClassesUseCase,
    private val resolveConflictUseCase: ResolveConflictUseCase,
    private val getCheckInRecordsUseCase: GetCheckInRecordsUseCase,
    private val syncCheckInRecordsUseCase: SyncCheckInRecordsUseCase,
    private val syncSchoolsUseCase: SyncSchoolsUseCase,
    private val syncFacesUseCase: SyncFacesUseCase,
    private val syncAssignmentsUseCase: SyncAssignmentsUseCase,
    private val getLocalDataCountUseCase: GetLocalDataCountUseCase,
    private val authRepository: AuthRepository,
    private val dataIntegrityRepository: DataIntegrityRepository,
    private val getFacesInClassUseCase: GetFacesInClassUseCase,
    private val sessionManager: SessionManager,
    private val syncRepository: SyncRepository
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
                triggerAutoSyncIfNeeded(userId)
            }
        }
    }

    private suspend fun triggerAutoSyncIfNeeded(userId: String) {
        val localCount = getLocalDataCountUseCase(userId)
        val lastSync = sessionManager.getLastSyncTime()
        val isStale = System.currentTimeMillis() - lastSync > 24 * 60 * 60 * 1000 // 24h stale logic
        
        if (localCount == 0 || isStale) {
            println("🔄 Persistence: Auto-sync triggered (localCount=$localCount, isStale=$isStale).")
            sync()
        }
    }

    val state: StateFlow<UiState<DashboardUiState>> = combine(
        _userFlow,
        _recentRecordsFlow,
        _sessionStudentsFlow,
        _assignedClassesFlow,
        syncRepository.isSyncing,
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

        UiState.Success(
            DashboardUiState(
                user = user,
                recentRecords = recentRecords,
                sessionStudents = sessionStudents,
                assignedClasses = assignedClasses,
                isSyncing = isSyncing,
                totalFaces = totalFaces,
                unassignedStudents = unassigned,
                brokenAssignments = broken,
                unsyncedRecords = unsynced,
                conflicts = conflicts,
                currentRole = user?.role ?: "USER",
                isApproved = user?.status == "ACTIVE"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun sync() {
        // Use Repository to trigger work
        syncRepository.forceSyncFromCloud()
        
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getCurrentUserId() ?: return@launch
            
            // 1. Restoring profile & memberships
            syncUserUseCase(userId)
            
            // 2. Restoring schools & classes
            syncSchoolsUseCase(userId)
            
            // 3. Restoring faces & assignments (tenant-scoped)
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId != null) {
                syncFacesUseCase()
                syncAssignmentsUseCase()
                syncCheckInRecordsUseCase()
            }
            
            sessionManager.saveLastSyncTime(System.currentTimeMillis())
            _uiEvent.emit(UiEvent.ShowSnackbar("Sinkronisasi Selesai!"))
            println("✅ DashboardViewModel: Comprehensive sync completed for user $userId")
        }
    }

    fun selectActiveClass(classId: String?) {
        viewModelScope.launch {
            val currentState = state.value
            if (currentState is UiState.Success) {
                val user = currentState.data.user ?: return@launch
                
                // 🔥 CLEAN ARCHITECTURE: Fetch via UseCase instead of direct DAO
                val userEntity = getUserByIdUseCase(user.userId)
                
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
