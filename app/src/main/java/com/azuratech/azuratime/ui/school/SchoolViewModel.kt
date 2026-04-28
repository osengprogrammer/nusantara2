package com.azuratech.azuratime.ui.school

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.domain.classes.usecase.GetAllClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.ReassignClassUseCase
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.domain.school.usecase.CreateSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.DeleteSchoolUseCase
import com.azuratech.azuratime.domain.school.usecase.GetSchoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SchoolUiState {
    object Loading : SchoolUiState()
    data class Success(val schools: List<School>) : SchoolUiState()
    data class Error(val error: AppError) : SchoolUiState()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSchoolsUseCase: GetSchoolsUseCase,
    private val createSchoolUseCase: CreateSchoolUseCase,
    private val deleteSchoolUseCase: DeleteSchoolUseCase,
    private val getAllClassesUseCase: GetAllClassesUseCase,
    private val assignClassToSchoolUseCase: com.azuratech.azuratime.domain.classes.usecase.AssignClassToSchoolUseCase,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val workspaceRepository: com.azuratech.azuratime.data.repo.WorkspaceRepository
) : ViewModel() {

    private val _accountId = MutableStateFlow(savedStateHandle.get<String>("accountId") ?: "")
    val accountId: StateFlow<String> = _accountId.asStateFlow()

    // 🔥 Observation of active school ID from SessionManager
    val activeSchoolId: StateFlow<String?> = sessionManager.activeSchoolIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.getActiveSchoolId())

    // 🔥 Observation of active school details from Repository
    val activeSchool: StateFlow<School?> = activeSchoolId
        .flatMapLatest { id ->
            if (id != null) {
                flow<School?> { emit(schoolRepository.getSchoolById(id)) }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<SchoolUiState> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            getSchoolsUseCase(id).onEach { result ->
                // 🔥 Auto-select first school if none active
                if (result is Result.Success && result.data.isNotEmpty() && sessionManager.getActiveSchoolId() == null) {
                    val firstSchool = result.data.first()
                    println("🚀 AUTO-INIT: Selecting first available school: ${firstSchool.name}")
                    selectSchool(firstSchool)
                }
            }.map { result ->
                when (result) {
                    is Result.Success -> SchoolUiState.Success(result.data)
                    is Result.Failure -> SchoolUiState.Error(result.error)
                    is Result.Loading -> SchoolUiState.Loading
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchoolUiState.Loading
        )

    // 🔥 Observation of all schools for pickers/selectors
    val allSchools: StateFlow<List<School>> = uiState.map { 
        if (it is SchoolUiState.Success) it.schools else emptyList() 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🔥 Added Available Classes flow for selection
    val availableClasses: StateFlow<List<ClassModel>> = _accountId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id ->
            getAllClassesUseCase(id).onEach { result ->
                if (result is Result.Success) {
                    println("📚 DEBUG: Loaded ${result.data.size} classes from DB for picker")
                }
            }.map { result ->
                if (result is Result.Success) result.data else emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAccountId(id: String) {
        if (id.isNotEmpty() && _accountId.value != id) {
            _accountId.value = id
        }
    }

    /**
     * 🔥 SELECT SCHOOL & PERSIST
     * Updates local session and cloud context through WorkspaceRepository.
     */
    fun selectSchool(school: School) {
        viewModelScope.launch {
            val userId = _accountId.value
            if (userId.isEmpty()) return@launch
            
            println("🔄 Switching school to: ${school.name} (${school.id})")
            sessionManager.saveActiveSchoolId(school.id)
            try {
                workspaceRepository.switchWorkspace(userId, school.id)
            } catch (e: Exception) {
                println("⚠️ Error switching workspace: ${e.message}")
            }
        }
    }

    fun createSchool(name: String, timezone: String, selectedClassIds: List<String> = emptyList()) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        println("💾 DEBUG: Creating school: $name with ${selectedClassIds.size} classes")
        viewModelScope.launch {
            val result = createSchoolUseCase(currentId, name, timezone)
            if (result is Result.Success) {
                val newSchoolId = result.data
                selectedClassIds.forEach { classId ->
                    assignClassToSchoolUseCase(newSchoolId, classId)
                }
                
                // 🔥 Auto-select if it's the first one
                if (sessionManager.getActiveSchoolId() == null) {
                    val newSchool = schoolRepository.getSchoolById(newSchoolId)
                    newSchool?.let { selectSchool(it) }
                }
            }
        }
    }

    fun deleteSchool(id: String) {
        val currentId = _accountId.value
        if (currentId.isEmpty()) return
        
        viewModelScope.launch {
            deleteSchoolUseCase(id, currentId)
        }
    }

    // Deprecated but kept for backward compatibility if needed, though we should update callers
    fun addSchool(accountId: String, name: String, timezone: String) {
        setAccountId(accountId)
        createSchool(name, timezone)
    }
}
