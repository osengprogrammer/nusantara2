package com.azuratech.azuratime.features.attendance.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.attendance.data.repo.ScannerRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessCheckInParams
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceUiState
import com.azuratech.azuratime.features.attendance.ui.capture.AttendanceSideEffect
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    application: Application,
    private val repository: ScannerRepository,
    private val checkInRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val uiStateStateFlow: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<AttendanceSideEffect>()
    val sideEffectFlow = _sideEffect.receiveAsFlow()

    private var gallery: List<Pair<String, FloatArray>> = emptyList()
    private var currentTeacherEmail: String = ""
    private var activeClassId: String? = null
    private var activeSchoolId: String? = null
    var activeClassName: String = ""

    // Gatekeeper: Prevents multiple concurrent processing
    private val isProcessing = AtomicBoolean(false)

    fun startScannerSession(email: String) {
        currentTeacherEmail = email
        viewModelScope.launch {
            val resolvedSchoolId = sessionManager.getActiveSchoolId()
            
            // 2. Fetch Session Data
            val (classId, className, schoolId) = repository.getSessionData(email, resolvedSchoolId)
            activeClassId = classId
            activeClassName = className
            activeSchoolId = schoolId
            
            if (schoolId != null) {
                gallery = repository.loadGallery(schoolId)
            } else {
                _uiState.value = AttendanceUiState.Error("Workspace Belum Dipilih")
            }
        }
    }

    fun processScannedFace(embedding: FloatArray) {
        if (isProcessing.getAndSet(true)) {
            return
        }

        viewModelScope.launch {
            if (activeSchoolId == null) {
                _uiState.value = AttendanceUiState.Error("Error: Context Hilang")
                enterCooldown()
                return@launch
            }

            _uiState.value = AttendanceUiState.Processing
            val matchedFaceId = repository.performMatch(embedding, gallery)

            if (matchedFaceId != null) {
                processAttendanceRecord(matchedFaceId)
            } else {
                handleUnregistered()
            }
        }
    }

    fun processScannedBarcode(barcode: String) {
        if (isProcessing.getAndSet(true)) {
            return
        }
        viewModelScope.launch {
            if (activeSchoolId == null) {
                _uiState.value = AttendanceUiState.Error("Error: Context Hilang")
                enterCooldown()
                return@launch
            }
            _uiState.value = AttendanceUiState.Processing
            processAttendanceRecord(barcode)
        }
    }

    private suspend fun processAttendanceRecord(scannedId: String) {
        val schoolId = activeSchoolId ?: return enterCooldown()
        val face = checkInRepository.getFaceById(scannedId, schoolId)
        
        if (face == null) {
            handleUnregistered()
            return
        }

        val studentClassIds = checkInRepository.getClassIdsForFace(scannedId, schoolId).firstOrNull() ?: emptyList()

        val params = ProcessCheckInParams(
            faceId = scannedId,
            studentName = face.name,
            teacherEmail = currentTeacherEmail,
            activeClassId = activeClassId,
            studentClassIds = studentClassIds
        )

        val result = checkInRepository.processCheckIn(params)

        when (result) {
            is Result.Success<AttendanceResult> -> {
                when (val checkInRes = result.data) {
                    is AttendanceResult.Success -> {
                        _uiState.value = AttendanceUiState.Success(checkInRes.name, alreadyCheckedIn = false)
                        _sideEffect.send(AttendanceSideEffect.Speak(checkInRes.message))
                    }
                    is AttendanceResult.AlreadyCheckedIn -> {
                        _uiState.value = AttendanceUiState.Success(checkInRes.name, alreadyCheckedIn = true)
                        _sideEffect.send(AttendanceSideEffect.Speak("${checkInRes.name}, sudah absen."))
                    }
                    is AttendanceResult.Rejected -> {
                        _uiState.value = AttendanceUiState.Error("${checkInRes.name}: Bukan Kelas Ini!")
                        _sideEffect.send(AttendanceSideEffect.Speak("${checkInRes.name}: Bukan Kelas Ini!"))
                    }
                    AttendanceResult.Unregistered -> handleUnregistered()
                }
            }
            is Result.Failure -> {
                _uiState.value = AttendanceUiState.Error(result.error.message ?: "Gagal Absen")
            }
            Result.Loading -> {
                _uiState.value = AttendanceUiState.Processing
            }
        }
        enterCooldown()
    }

    private suspend fun handleUnregistered() {
        _uiState.value = AttendanceUiState.Error("Wajah Tidak Dikenal")
        _sideEffect.send(AttendanceSideEffect.Speak("Wajah Tidak Dikenal"))
        enterCooldown()
    }

    private suspend fun enterCooldown(duration: Long = 2500) {
        delay(duration)
        _uiState.value = AttendanceUiState.Idle
        isProcessing.set(false)
    }
}
