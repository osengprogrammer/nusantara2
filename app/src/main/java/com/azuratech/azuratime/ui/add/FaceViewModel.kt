package com.azuratech.azuratime.ui.add

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// 🔥 PASTIKAN IMPORT DI BAWAH INI SESUAI LOKASI FILE REPOSITORY ANDA
import com.azuratech.azuratime.data.repository.FaceRepository
import com.azuratech.azuratime.data.repository.RegisterResult
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🛠️ FACE VIEW MODEL (Hilt Version)
 */
@HiltViewModel
class FaceViewModel @Inject constructor(
    application: Application,
    private val repository: FaceRepository, // Parameter ke-2 (yang error di KSP)
    private val sessionManager: SessionManager // Parameter ke-3
) : AndroidViewModel(application) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val faceList: StateFlow<List<com.azuratech.azuratime.data.local.FaceWithDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { repository.allFacesWithDetailsFlow }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrolledFaceList: StateFlow<List<com.azuratech.azuratime.data.local.FaceEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { repository.facesForScanningFlow }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    fun getFacesInClassFlow(classId: String): Flow<List<com.azuratech.azuratime.data.local.FaceEntity>> = 
        repository.getFacesInClassFlow(classId)

    suspend fun getAssignments(faceId: String) = 
        repository.getAssignmentsForFace(faceId)

    fun registerFace(
        inputId: String, 
        classId: String, 
        name: String, 
        embedding: FloatArray,
        photoBitmap: Bitmap? = null,
        onSuccess: () -> Unit, 
        onDuplicate: (existingName: String) -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerFace(inputId, classId, name, embedding, photoBitmap)
            withContext(Dispatchers.Main) {
                when (result) {
                    is RegisterResult.Success -> onSuccess()
                    is RegisterResult.Duplicate -> onDuplicate(result.name)
                    is RegisterResult.Error -> onError(result.message)
                }
            }
        }
    }

    fun deleteFace(face: com.azuratech.azuratime.data.local.FaceEntity) { 
        viewModelScope.launch { repository.deleteFace(face) } 
    }

    fun updateEmployeeClass(faceId: String, classId: String?) {
        viewModelScope.launch { repository.updateEmployeeClass(faceId, classId) }
    }

    fun updateFace(face: com.azuratech.azuratime.data.local.FaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch { 
            repository.updateFaceBasic(face)
            withContext(Dispatchers.Main) { onComplete() } 
        }
    }

    fun updateFaceWithPhoto(
        face: com.azuratech.azuratime.data.local.FaceEntity, 
        photoBitmap: Bitmap?, 
        embedding: FloatArray, 
        onComplete: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateFaceWithPhoto(face, photoBitmap, embedding)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onComplete() },
                    onFailure = { onError("Gagal: ${it.message}") }
                )
            }
        }
    }
}