package com.azuratech.azuratime.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.BiometricFaceEntity
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repo.AdminRepository
import com.azuratech.azuratime.data.repo.AuthRepository
import com.azuratech.azuratime.data.repo.StaffAccountRepository
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.repo.BiometricFaceRepository
import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.core.UiEvent
import kotlinx.coroutines.channels.Channel
import com.azuratech.azuratime.core.session.SessionManager
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
    private val userRepository: StaffAccountRepository,
    private val faceRepository: BiometricFaceRepository,
    private val checkInRepository: CheckInRepository,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
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

    private val _userFlow = sessionManager.currentUserIdFlow
        .flatMapLatest { userId -> 
            if (userId != null) {
                userRepository.observeUserEntity(userId)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _recentRecordsFlow = sessionManager.activeSchoolIdFlow
        .flatMapLatest { schoolId ->
            if (schoolId != null) {
                checkInRepository.getCheckInRecords("", null, null, null, null, emptyList(), schoolId).map { it.take(5) }
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
        _userFlow
    ) { schoolId, user ->
        schoolId to user
    }.flatMapLatest { (schoolId, user) ->
        if (schoolId != null && user != null) {
            schoolRepository.observeClasses(schoolId).map { result ->
                val allClasses = if (result is Result.Success) result.data else emptyList()
                val membership = user.memberships[schoolId]
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

    private val _sessionStudentsFlow = _userFlow
        .flatMapLatest { user ->
            val activeClassId = user?.activeClassId
            val schoolId = user?.activeSchoolId
            if (activeClassId != null && schoolId != null) {
                faceRepository.getFacesInClassFlow(activeClassId, schoolId)
            } else {
                flowOf(emptyList())
            }
        }


    init {
        // 🔥 FIX: React to ID changes instead of one-shot check
        sessionManager.currentUserIdFlow
            .filterNotNull()
            .onEach { userId -> 
                println("🔍 Dashboard: ID detected ($userId), triggering sync...")
                triggerAutoSyncIfNeeded(userId) 
            }
            .launchIn(viewModelScope)
    }

    private suspend fun triggerAutoSyncIfNeeded(userId: String) {
        val lastSync = sessionManager.getLastSyncTime()
        val isStale = System.currentTimeMillis() - lastSync > 24 * 60 * 60 * 1000 // 24h stale logic
        
        if (isStale) {
            println("🔄 Persistence: Auto-sync triggered (isStale=$isStale).")
            sync()
        }
    }

    val state: StateFlow<UiState<DashboardUiState>> = combine(
        _userFlow,
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
        val user = args[0] as UserEntity?
        @Suppress("UNCHECKED_CAST")
        val recentRecords = args[1] as List<CheckInRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val sessionStudents = args[2] as List<BiometricFaceEntity>
        @Suppress("UNCHECKED_CAST")
        val assignedClasses = args[3] as List<ClassModel>
        @Suppress("UNCHECKED_CAST")
        val allClasses = args[4] as List<ClassModel>
        val isSyncing = args[5] as Boolean
        val totalFaces = args[6] as Int
        val unassigned = args[7] as Int
        val broken = args[8] as Int
        val unsynced = args[9] as Int
        @Suppress("UNCHECKED_CAST")
        val conflicts = args[10] as List<AttendanceConflict>

        val isReady = (user != null) && (isSyncing == false)

        println("🔄 Dashboard combine: user=${user?.userId ?: "NULL"}, isReady=$isReady")

        UiState.Success(
            DashboardUiState(
                user = user,
                recentRecords = recentRecords,
                sessionStudents = sessionStudents,
                assignedClasses = assignedClasses,
                allClasses = allClasses,
                isSyncing = isSyncing,
                isReady = isReady,
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
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getCurrentUserId() ?: return@launch
            
            // 1. Restoring profile & memberships
            userRepository.syncUser(userId)
            
            // 2. Restoring faces & assignments (tenant-scoped)
            val schoolId = sessionManager.getActiveSchoolId()
            if (schoolId != null) {
                val faceSyncResult = faceRepository.syncFaces()
                faceRepository.syncAssignments()
                checkInRepository.syncRecords()

                if (faceSyncResult is Result.Failure) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("Gagal sinkron data wajah: ${faceSyncResult.error.message}"))
                }
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
                userRepository.getUserDao().getUserById(user.userId)?.let {
                    userRepository.getUserDao().updateUser(it.copy(activeClassId = classId))
                }
            }
        }
    }

    fun resolveConflict(conflict: AttendanceConflict, useCloud: Boolean) {
        viewModelScope.launch {
            checkInRepository.resolveConflict(conflict.conflictId, useCloud)
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
