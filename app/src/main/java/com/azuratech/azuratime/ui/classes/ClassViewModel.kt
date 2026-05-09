package com.azuratech.azuratime.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ui.util.UiState
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

/**
 * 🛠️ CLASS VIEW MODEL - Refactored to match School pattern
 */
@HiltViewModel
class ClassViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val registrationRepository: com.azuratech.azuratime.data.repo.RegistrationRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // 🔥 NEW: Reactive School ID Flow
    private val activeSchoolIdFlow = sessionManager.activeSchoolIdFlow
        .onStart { 
            val initial = savedStateHandle.get<String>("schoolId") ?: sessionManager.getActiveSchoolId()
            emit(initial) 
        }
        .filterNotNull()
        .distinctUntilChanged()

    private val schoolId: String 
        get() = sessionManager.getActiveSchoolId() ?: ""
        
    private val accountId: String = savedStateHandle.get<String>("accountId")
        ?: sessionManager.getCurrentUserId() ?: ""

    // 🔥 User Flow for UI - Using UserEntity for SSOT
    val user: StateFlow<UserEntity?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { userRepository.observeUserEntity(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // =====================================================
    // 📊 CLASS FLOWS (State Management)
    // =====================================================

    val uiStateStateFlow: StateFlow<UiState<List<ClassModel>>> = activeSchoolIdFlow
        .flatMapLatest { id -> schoolRepository.observeClasses(id) }
        .map { result ->
            when(result) {
                is Result.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Result.Failure -> UiState.Error(result.error.message ?: "Unknown error")
                is Result.Loading -> UiState.Loading
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val classesStateFlow: StateFlow<List<ClassModel>> = uiStateStateFlow.map {
        if (it is UiState.Success) it.data else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🔥 Added Available Classes Flow - Simplified to static for now
    val availableClassesStateFlow: StateFlow<List<String>> = flowOf(listOf(
        "10-IPA-1", "10-IPA-2", "10-IPA-3",
        "10-IPS-1", "10-IPS-2", "10-IPS-3",
        "11-IPA-1", "11-IPA-2", "11-IPA-3",
        "11-IPS-1", "11-IPS-2", "11-IPS-3",
        "12-IPA-1", "12-IPA-2", "12-IPA-3",
        "12-IPS-1", "12-IPS-2", "12-IPS-3"
    )).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🔥 Added All Classes for Account Flow
    val allAccountClassesStateFlow: StateFlow<UiState<List<ClassModel>>> = schoolRepository.observeAllClassesForAccount(accountId)
        .map { result ->
            when(result) {
                is Result.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Result.Failure -> UiState.Error(result.error.message ?: "Unknown error")
                is Result.Loading -> UiState.Loading
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val schoolsStateFlow: StateFlow<List<School>> = schoolRepository.observeSchools(accountId)
        .map { result ->
            if (result is Result.Success) result.data else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // ➕ CRUD OPERATIONS
    // =====================================================

    fun createClass(name: String, schoolId: String? = null, _grade: String = "") {
        val targetSchoolId = schoolId ?: this.schoolId
        viewModelScope.launch {
            val classModel = ClassModel(
                id = "cls_${System.currentTimeMillis()}",
                name = name,
                schoolId = targetSchoolId ?: "",
                grade = _grade,
                teacherId = null,
                studentCount = 0,
                createdAt = System.currentTimeMillis()
            )
            val result = schoolRepository.saveClass(accountId, targetSchoolId, classModel)
            when (result) {
                is Result.Success -> _uiEvent.emit(UiEvent.ShowSnackbar("Kelas '$name' berhasil dibuat!"))
                is Result.Failure -> _uiEvent.emit(UiEvent.ShowSnackbar("Gagal membuat kelas: ${result.error.message}"))
                else -> Unit
            }
        }
    }

    fun addClass(name: String) {
        createClass(name)
    }

    fun updateClass(classId: String, newName: String) {
        viewModelScope.launch {
            // Fetch existing to preserve other fields
            val allClasses = classesStateFlow.value
            val existing = allClasses.find { it.id == classId } ?: return@launch
            val updated = existing.copy(name = newName)
            schoolRepository.saveClass(accountId, null, updated)
        }
    }

    fun importClassesFromCsv(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            registrationRepository.processCsv(uri.toString(), "CLASS").collect { }
            onComplete()
        }
    }

    fun deleteClass(
        classId: String,
        onFailure: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = schoolRepository.deleteClass(accountId, schoolId, classId)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> onSuccess()
                    is Result.Failure -> onFailure(result.error.message ?: "Gagal menghapus kelas")
                    else -> Unit
                }
            }
        }
    }

    fun reassignClass(classId: String, newSchoolId: String) {
        viewModelScope.launch {
            schoolRepository.reassignClass(accountId, classId, newSchoolId)
        }
    }
}
