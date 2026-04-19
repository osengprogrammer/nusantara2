package com.azuratech.azuratime.ui.add

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.azuratech.azuratime.domain.face.RegisterResult
import com.azuratech.azuratime.domain.face.RegisterFaceUseCase
import com.azuratech.azuratime.domain.face.GetFacesWithDetailsUseCase
import com.azuratech.azuratime.domain.face.GetEnrolledFacesUseCase
import com.azuratech.azuratime.domain.face.DeleteFaceUseCase
import com.azuratech.azuratime.domain.face.UpdateEmployeeClassUseCase
import com.azuratech.azuratime.domain.face.UpdateFaceUseCase
import com.azuratech.azuratime.domain.face.GetFacesInClassUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.azuratech.azuratime.domain.result.Result


import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🛠️ FACE VIEW MODEL (Hilt Version)
 */
@HiltViewModel
class FaceViewModel @Inject constructor(
    application: Application,
    private val getFacesWithDetailsUseCase: GetFacesWithDetailsUseCase,
    private val getEnrolledFacesUseCase: GetEnrolledFacesUseCase,
    private val registerFaceUseCase: RegisterFaceUseCase,
    private val deleteFaceUseCase: DeleteFaceUseCase,
    private val updateEmployeeClassUseCase: UpdateEmployeeClassUseCase,
    private val updateFaceUseCase: UpdateFaceUseCase,
    private val getFacesInClassUseCase: GetFacesInClassUseCase,
    private val sessionManager: SessionManager // Parameter ke-3
) : AndroidViewModel(application) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val faceList: StateFlow<List<com.azuratech.azuratime.data.local.FaceWithDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { getFacesWithDetailsUseCase() }
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrolledFaceList: StateFlow<List<com.azuratech.azuratime.data.local.FaceEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { getEnrolledFacesUseCase() }
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    fun getFacesInClassFlow(classId: String): Flow<List<com.azuratech.azuratime.data.local.FaceEntity>> = 
        getFacesInClassUseCase(classId).map { result ->
            when (result) {
                is Result.Success -> result.data
                else -> emptyList()
            }
        }



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
            val result = registerFaceUseCase(inputId, classId, name, embedding, photoBitmap)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { regResult ->
                        when (regResult) {
                            is RegisterResult.Success -> onSuccess()
                            is RegisterResult.Duplicate -> onDuplicate(regResult.name)
                            is RegisterResult.Error -> onError(regResult.message)
                        }
                    },
                    onFailure = { onError(it.message ?: "Gagal registrasi") }
                )
            }
        }
    }

    fun deleteFace(face: com.azuratech.azuratime.data.local.FaceEntity) { 
        viewModelScope.launch { 
            try {
                deleteFaceUseCase(face) 
            } catch (e: Exception) {
                android.util.Log.e("FaceViewModel", "Gagal hapus: ${e.message}")
            }
        } 
    }

    fun updateEmployeeClass(faceId: String, classId: String?) {
        viewModelScope.launch { updateEmployeeClassUseCase(faceId, classId) }
    }

    fun updateFace(face: com.azuratech.azuratime.data.local.FaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch { 
            updateFaceUseCase(face)
            withContext(Dispatchers.Main) { onComplete() } 
        }
    }

}