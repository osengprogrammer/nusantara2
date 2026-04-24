package com.azuratech.azuratime.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.domain.classes.usecase.DeleteClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
import com.azuratech.azuratime.domain.classes.usecase.UpdateClassUseCase
import com.azuratech.azuratime.domain.classes.usecase.ImportClassesUseCase
import com.azuratech.azuratime.domain.result.Result
import com.azuratech.azuratime.ui.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

/**
 * 🛠️ CLASS VIEW MODEL - Migrated to UseCases
 */
@HiltViewModel
class ClassViewModel @Inject constructor(
    private val getClassesUseCase: GetClassesUseCase,
    private val updateClassUseCase: UpdateClassUseCase,
    private val deleteClassUseCase: DeleteClassUseCase,
    private val importClassesUseCase: ImportClassesUseCase
) : ViewModel() {

    // =====================================================
    // 📊 CLASS FLOWS (State Management)
    // =====================================================

    private val _uiState = MutableStateFlow<UiState<List<ClassEntity>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ClassEntity>>> = getClassesUseCase()
        .map { result ->
            when(result) {
                is Result.Success -> {
                    if (result.data.isEmpty()) UiState.Empty
                    else UiState.Success(result.data)
                }
                is Result.Failure -> UiState.Error(result.error.message ?: "Unknown error")
                is Result.Loading -> UiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    val classes: StateFlow<List<ClassEntity>> = uiState.map {
        if (it is UiState.Success) it.data else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // ➕ CRUD OPERATIONS
    // =====================================================

    fun addClass(name: String) {
        viewModelScope.launch {
            updateClassUseCase(name)
        }
    }

    fun updateClass(classId: String, newName: String) {
        viewModelScope.launch {
            updateClassUseCase(newName, classId)
        }
    }

    fun importClassesFromCsv(uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = importClassesUseCase(uri.toString())
            // Even if it fails, we call onComplete to stop loading UI
            // The result handling for errors could be added here if we had a snackbar state
            onComplete()
        }
    }

    fun deleteClass(
        classEntity: ClassEntity,
        onFailure: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = deleteClassUseCase(classEntity.id)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> onSuccess()
                    is Result.Failure -> onFailure(result.error.message ?: "Gagal menghapus kelas")
                    else -> Unit
                }
            }
        }
    }
}
