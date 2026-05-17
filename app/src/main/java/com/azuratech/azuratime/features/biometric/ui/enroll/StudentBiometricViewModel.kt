package com.azuratech.azuratime.features.biometric.ui.enroll

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository
import com.azuratech.azuratime.core.ui.UiEvent
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
import kotlinx.coroutines.flow.stateIn
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.data.local.StudentBiometricDetails

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 🛠️ STUDENT BIOMETRIC VIEW MODEL
 * Handles student registration and biometric management.
 */
@HiltViewModel
class StudentBiometricViewModel @Inject constructor(
    application: Application,
    private val biometricRepository: StudentBiometricRepository,
    private val sessionManager: SessionManager,
) : AndroidViewModel(application) {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentRosterFlow: StateFlow<List<StudentBiometricDetails>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getStudentsWithDetailsFlow(schoolId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrolledStudentFlow: StateFlow<List<StudentBiometricEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId -> biometricRepository.getEnrolledStudentsFlow(schoolId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun getStudentsInClassFlow(classId: String): Flow<List<StudentBiometricEntity>> =
        biometricRepository.getStudentsInClassFlow(classId, sessionManager.getActiveSchoolId() ?: "")

    fun registerStudentBiometric(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBitmap: Bitmap? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val photoBytes = photoBitmap?.let { bitmapToByteArray(it) }
            val schoolId = sessionManager.getActiveSchoolId() ?: ""

            // 🔥 AI Friendly: Unified Identity
            val studentId = inputId
            val faceId = inputId

            val profile = StudentProfile(
                studentId = studentId,
                name = name,
                schoolId = schoolId,
                classIds = listOf(classId),
                faceId = faceId, // Explicitly unified
                embedding = embedding,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )

            val result = biometricRepository.saveStudentProfile(profile, photoBytes)
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

    fun deleteStudentBiometric(biometric: StudentBiometricEntity) {
        viewModelScope.launch {
            val result = biometricRepository.deleteStudent(biometric.studentId)
            if (result is Result.Failure) {
                android.util.Log.e("StudentBiometricVM", "Gagal hapus: ${result.error.message}")
            }
        }
    }

    fun updateStudentClass(studentId: String, classId: String?) {
        viewModelScope.launch {
            val result = biometricRepository.updateStudentClass(studentId, classId)
            if (result is Result.Failure) {
                android.util.Log.e("StudentBiometricVM", "Gagal update kelas: ${result.error.message}")
            }
        }
    }

    fun updateBiometric(biometric: StudentBiometricEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            val profile = StudentProfile(
                studentId = biometric.studentId,
                name = biometric.name,
                schoolId = biometric.schoolId,
                classIds = emptyList(),
                faceId = biometric.studentId,
                embedding = biometric.embedding,
                photoUrl = biometric.photoUrl,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )

            val result = biometricRepository.saveStudentProfile(profile)
            withContext(Dispatchers.Main) {
                if (result is Result.Failure) {
                    android.util.Log.e("StudentBiometricVM", "Gagal update biometric: ${result.error.message}")
                }
                onComplete()
            }
        }
    }

    fun refreshBiometrics() {
        viewModelScope.launch {
            when (val result = biometricRepository.syncBiometrics()) {
                is Result.Success -> _uiEvent.emit(UiEvent.ShowSnackbar("Data biometrik berhasil diperbarui"))
                is Result.Failure -> _uiEvent.emit(UiEvent.ShowSnackbar("Gagal sinkron: ${result.error.message}"))
                else -> {}
            }
        }
    }
}
