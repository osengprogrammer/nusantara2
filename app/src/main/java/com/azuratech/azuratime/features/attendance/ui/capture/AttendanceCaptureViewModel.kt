package com.azuratech.azuratime.features.attendance.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.data.repo.BiometricScannerRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.student.domain.model.StudentProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AttendanceCaptureViewModel @Inject constructor(
    application: Application,
    private val repository: BiometricScannerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionManager: SessionManager,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttendanceCheckInUiState())
    val uiState: StateFlow<AttendanceCheckInUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<AttendanceSideEffect>()
    val sideEffectFlow = _sideEffect.receiveAsFlow()

    private var gallery: List<Pair<String, FloatArray>> = emptyList()
    private var currentTeacherEmail: String = ""
    private var activeClassId: String? = null

    // Gatekeeper: Prevents multiple concurrent processing
    private val isProcessing = AtomicBoolean(false)

    // Tracking for deduplication to prevent "double output"
    private var lastProcessedStudentId: String? = null
    private var lastProcessedTime: Long = 0L
    private val REPEAT_SCAN_SUPPRESSION_MS = 600_000L // 10 minutes

    fun onEvent(event: AttendanceCheckInUiEvent) {
        when (event) {
            is AttendanceCheckInUiEvent.StartScan -> startScannerSession(event.accountEmail, event.mode)
            is AttendanceCheckInUiEvent.BarcodeDetected -> processScannedBarcode(event.code)
            is AttendanceCheckInUiEvent.FaceMatched -> processScannedBiometric(event.embedding)
            is AttendanceCheckInUiEvent.ManualEntryConfirmed -> { /* Logic for manual confirm if needed */ }
            is AttendanceCheckInUiEvent.GrantPermission -> _uiState.update { it.copy(cameraPermissionGranted = event.granted) }
            AttendanceCheckInUiEvent.Retry -> resetScanningState()
            AttendanceCheckInUiEvent.NavigateBack -> viewModelScope.launch { _sideEffect.send(AttendanceSideEffect.NavigateBack) }
        }
    }

    private fun resetScanningState() {
        _uiState.update { it.copy(error = null, studentProfile = null, isScanning = true, isLoading = false) }
        isProcessing.set(false)
    }

    private fun startScannerSession(email: String, mode: ScanMode) {
        currentTeacherEmail = email
        _uiState.update { it.copy(isLoading = true, error = null, scanMode = mode) }

        viewModelScope.launch {
            val resolvedSchoolId = sessionManager.getActiveSchoolId()

            val (classId, className, schoolId) = repository.getSessionData(email, resolvedSchoolId)
            activeClassId = classId

            if (schoolId != null) {
                gallery = repository.loadGallery(schoolId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeClassName = className,
                        activeSchoolId = schoolId,
                        isScanning = true,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Workspace Belum Dipilih", isScanning = false) }
            }
        }
    }

    private fun processScannedBiometric(embedding: FloatArray) {
        if (isProcessing.getAndSet(true)) return

        viewModelScope.launch {
            val schoolId = _uiState.value.activeSchoolId
            if (schoolId == null) {
                handleError("Error: Context Hilang")
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, isScanning = false, error = null, studentProfile = null) }
            val matchedStudentId = repository.performMatch(embedding, gallery)

            if (matchedStudentId != null) {
                processAttendanceRecord(matchedStudentId, schoolId)
            } else {
                handleUnregistered()
            }
        }
    }

    private fun processScannedBarcode(barcode: String) {
        if (isProcessing.getAndSet(true)) return

        viewModelScope.launch {
            val schoolId = _uiState.value.activeSchoolId
            if (schoolId == null) {
                handleError("Error: Context Hilang")
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, isScanning = false, error = null, studentProfile = null, scannedResult = barcode) }
            processAttendanceRecord(barcode, schoolId)
        }
    }

    private suspend fun processAttendanceRecord(scannedId: String, schoolId: String) {
        val currentTime = System.currentTimeMillis()
        if (scannedId == lastProcessedStudentId && (currentTime - lastProcessedTime < REPEAT_SCAN_SUPPRESSION_MS)) {
            // Deduplication logic
            isProcessing.set(false)
            _uiState.update { it.copy(isLoading = false, isScanning = true) }
            return
        }

        val studentBiometric = attendanceRepository.getStudentBiometricById(scannedId, schoolId)
        if (studentBiometric == null) {
            handleUnregistered()
            return
        }

        val studentClassIds = attendanceRepository.getClassIdsForStudent(scannedId, schoolId).firstOrNull() ?: emptyList()

        val params = ProcessAttendanceParams(
            studentId = scannedId,
            studentName = studentBiometric.name,
            accountEmail = currentTeacherEmail,
            activeClassId = activeClassId,
            studentClassIds = studentClassIds,
        )

        val result = attendanceRepository.processAttendance(params)

        when (result) {
            is Result.Success -> {
                when (val attendanceRes = result.data) {
                    is AttendanceResult.Success -> {
                        handleCheckInSuccess(scannedId, attendanceRes.name, attendanceRes.message, false)
                    }
                    is AttendanceResult.AlreadyCheckedIn -> {
                        handleCheckInSuccess(scannedId, attendanceRes.name, "${attendanceRes.name}, sudah absen.", true)
                    }
                    is AttendanceResult.Rejected -> {
                        handleError("${attendanceRes.name}: Bukan Kelas Ini!")
                    }
                    AttendanceResult.Unregistered -> handleUnregistered()
                }
            }
            is Result.Failure -> {
                handleError(result.error.message ?: "Gagal Absen")
            }
            is Result.Loading -> { /* Not used here */ }
        }
    }

    private suspend fun handleCheckInSuccess(studentId: String, name: String, speakMessage: String, alreadyCheckedIn: Boolean) {
        lastProcessedStudentId = studentId
        lastProcessedTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isLoading = false,
                studentProfile = StudentProfile(studentId = studentId, name = name, schoolId = _uiState.value.activeSchoolId ?: ""),
                isAlreadyCheckedIn = alreadyCheckedIn,
            )
        }
        _sideEffect.send(AttendanceSideEffect.Speak(speakMessage))
        enterCooldown()
    }

    private suspend fun handleUnregistered() {
        handleError("Identitas Tidak Dikenal")
    }

    private suspend fun handleError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message, isScanning = false) }
        _sideEffect.send(AttendanceSideEffect.Speak(message))
        enterCooldown()
    }

    private suspend fun enterCooldown(duration: Long = 4000) {
        delay(duration)
        resetScanningState()
    }
}
