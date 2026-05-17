package com.azuratech.azuratime.features.attendance.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.data.repo.BiometricScannerRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
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

    private val _uiState = MutableStateFlow(AttendanceCaptureUiState())
    val uiState: StateFlow<AttendanceCaptureUiState> = _uiState.asStateFlow()

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
    private val REPEAT_SCAN_SUPPRESSION_MS = 600_000L // 10 minutes (60s * 10)

    fun onEvent(event: AttendanceCaptureUiEvent) {
        when (event) {
            is AttendanceCaptureUiEvent.StartScan -> startScannerSession(event.accountEmail)
            is AttendanceCaptureUiEvent.BarcodeDetected -> processScannedBarcode(event.code)
            is AttendanceCaptureUiEvent.FaceDetected -> processScannedBiometric(event.embedding)
            is AttendanceCaptureUiEvent.GrantPermission -> _uiState.update { it.copy(cameraPermissionGranted = event.granted) }
            AttendanceCaptureUiEvent.Retry -> {
                _uiState.update { it.copy(error = null, studentProfile = null, isScanning = true) }
                isProcessing.set(false)
            }
        }
    }

    private fun startScannerSession(email: String) {
        currentTeacherEmail = email
        _uiState.update { it.copy(isLoading = true, error = null) }

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
            _uiState.update { it.copy(isLoading = true, isScanning = false, error = null, studentProfile = null, scannedCode = barcode) }
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

        attendanceRepository.processAttendance(params)
            .onSuccess { attendanceRes ->
                when (attendanceRes) {
                    is AttendanceResult.Success -> {
                        lastProcessedStudentId = scannedId
                        lastProcessedTime = System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                studentProfile = StudentProfile(scannedId, attendanceRes.name, false),
                            )
                        }
                        _sideEffect.send(AttendanceSideEffect.Speak(attendanceRes.message))
                        enterCooldown()
                    }
                    is AttendanceResult.AlreadyCheckedIn -> {
                        lastProcessedStudentId = scannedId
                        lastProcessedTime = System.currentTimeMillis()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                studentProfile = StudentProfile(scannedId, attendanceRes.name, true),
                            )
                        }
                        _sideEffect.send(AttendanceSideEffect.Speak("${attendanceRes.name}, sudah absen."))
                        enterCooldown()
                    }
                    is AttendanceResult.Rejected -> {
                        handleError("${attendanceRes.name}: Bukan Kelas Ini!")
                    }
                    AttendanceResult.Unregistered -> handleUnregistered()
                }
            }
            .onFailure { error ->
                handleError(error.message ?: "Gagal Absen")
            }
    }

    private suspend fun handleUnregistered() {
        handleError("Wajah Tidak Dikenal")
    }

    private suspend fun handleError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message, isScanning = false) }
        _sideEffect.send(AttendanceSideEffect.Speak(message))
        enterCooldown()
    }

    private suspend fun enterCooldown(duration: Long = 4000) {
        delay(duration)
        _uiState.update { it.copy(error = null, studentProfile = null, isScanning = true) }
        isProcessing.set(false)
    }
}
