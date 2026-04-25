package com.azuratech.azuratime.ui.add

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.azuratech.azuraengine.face.RegisterResult
import com.azuratech.azuratime.domain.face.usecase.*
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
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceWithDetails

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
    private val syncFacesUseCase: SyncFacesUseCase,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val faceList: StateFlow<List<FaceWithDetails>> = sessionManager.activeSchoolIdFlow
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
    val enrolledFaceList: StateFlow<List<FaceEntity>> = sessionManager.activeSchoolIdFlow
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

    fun getFacesInClassFlow(classId: String): Flow<List<FaceEntity>> = 
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
            val photoBytes = photoBitmap?.let { bitmapToByteArray(it) }
            val result = registerFaceUseCase(inputId, classId, name, embedding, photoBytes)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        when (val regResult = result.data) {
                            is RegisterResult.Success -> onSuccess()
                            is RegisterResult.Duplicate -> onDuplicate(regResult.name)
                            is RegisterResult.Error -> onError(regResult.message)
                        }
                    }
                    is Result.Failure -> onError(result.error.message ?: "Gagal registrasi")
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    fun deleteFace(face: FaceEntity) { 
        viewModelScope.launch { 
            val result = deleteFaceUseCase(face.faceId)
            if (result is Result.Failure) {
                android.util.Log.e("FaceViewModel", "Gagal hapus: ${result.error.message}")
            }
        } 
    }

    fun updateEmployeeClass(faceId: String, classId: String?) {
        viewModelScope.launch { 
            val result = updateEmployeeClassUseCase(faceId, classId)
            if (result is Result.Failure) {
                android.util.Log.e("FaceViewModel", "Gagal update kelas: ${result.error.message}")
            }
        }
    }

    fun updateFace(face: FaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch { 
            val result = updateFaceUseCase(face)
            withContext(Dispatchers.Main) { 
                if (result is Result.Failure) {
                    android.util.Log.e("FaceViewModel", "Gagal update face: ${result.error.message}")
                }
                onComplete() 
            } 
        }
    }

    fun refreshFaces() {
        viewModelScope.launch {
            syncFacesUseCase()
        }
    }
}