package com.azuratech.azuratime.ui.add

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.azuratech.azuraengine.face.RegisterResult
import com.azuratech.azuratime.data.repo.BiometricFaceRepository
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.BiometricFaceEntity
import com.azuratech.azuratime.data.local.FaceWithDetails

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus

/**
 * 🛠️ FACE VIEW MODEL (Hilt Version)
 */
@HiltViewModel
class FaceViewModel @Inject constructor(
    application: Application,
    private val faceRepository: BiometricFaceRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<FaceWithDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> faceRepository.getFacesWithDetailsFlow(schoolId) }
        .map { it }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrolledStudentFlow: StateFlow<List<BiometricFaceEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> faceRepository.getEnrolledFacesFlow(schoolId) }
        .map { it }
        .stateIn(
            scope = viewModelScope, 
            started = SharingStarted.WhileSubscribed(5000), 
            initialValue = emptyList()
        )

    fun getFacesInClassFlow(classId: String): Flow<List<BiometricFaceEntity>> = 
        faceRepository.getFacesInClassFlow(classId, sessionManager.getActiveSchoolId() ?: "").map { it }

    fun registerFace(
        inputId: String, 
        classId: String, 
        name: String, 
        embedding: FloatArray,
        photoBitmap: Bitmap? = null,
        onSuccess: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val photoBytes = photoBitmap?.let { bitmapToByteArray(it) }
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            
            // Generate IDs
            val studentId = if (inputId.contains("--")) inputId.split("--").last() else inputId
            val faceId = if (inputId.contains("--")) inputId else "${classId}--${inputId}"

            val profile = StudentProfile(
                studentId = studentId,
                name = name,
                schoolId = schoolId,
                classIds = listOf(classId),
                faceId = faceId,
                embedding = embedding,
                syncStatus = SyncStatus.PENDING_UPDATE
            )

            val result = faceRepository.saveStudentProfile(profile, photoBytes)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success<Unit> -> onSuccess()
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

    fun deleteFace(face: BiometricFaceEntity) { 
        viewModelScope.launch { 
            val result = faceRepository.deleteFace(face.faceId)
            if (result is Result.Failure) {
                android.util.Log.e("FaceViewModel", "Gagal hapus: ${result.error.message}")
            }
        } 
    }

    fun updateEmployeeClass(faceId: String, classId: String?) {
        viewModelScope.launch { 
            val result = faceRepository.updateFaceClass(faceId, classId)
            if (result is Result.Failure) {
                android.util.Log.e("FaceViewModel", "Gagal update kelas: ${result.error.message}")
            }
        }
    }

    fun updateFace(face: BiometricFaceEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            val profile = StudentProfile(
                studentId = face.studentId ?: face.faceId,
                name = face.name,
                schoolId = face.schoolId,
                classIds = emptyList(), // Class info not available here, repository will handle it or keep existing
                faceId = face.faceId,
                embedding = face.embedding,
                photoUrl = face.photoUrl,
                syncStatus = SyncStatus.PENDING_UPDATE
            )
            
            val result = faceRepository.saveStudentProfile(profile)
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
            when (val result = faceRepository.syncFaces()) {
                is Result.Success -> _uiEvent.emit(UiEvent.ShowSnackbar("Data wajah berhasil diperbarui"))
                is Result.Failure -> _uiEvent.emit(UiEvent.ShowSnackbar("Gagal sinkron: ${result.error.message}"))
                else -> {}
            }
        }
    }
}