package com.azuratech.azuratime.features.attendance.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.attendance.domain.repository.BiometricScannerRepository
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

/**
 * 🚀 ATTENDANCE CAPTURE VIEW MODEL (v3.2.0-ai-native)
 * Manages biometric and barcode attendance check-in.
 */
@HiltViewModel
class AttendanceCaptureViewModel @Inject constructor(
    application: Application,
    private val repository: BiometricScannerRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionManager: SessionManager,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow(AttendanceCheckInUiState())
    val uiStateFlow: StateFlow<AttendanceCheckInUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = Channel<AttendanceCaptureUiEffect>()
    val uiEffectFlow = _uiEffectFlow.receiveAsFlow()

    private var gallery: List<Pair<String, FloatArray>> = emptyList()
    private var currentAccountEmail: String = ""
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
            is AttendanceCheckInUiEvent.GrantPermission -> _uiStateFlow.update { it.copy(cameraPermissionGranted = event.granted) }
            AttendanceCheckInUiEvent.Retry -> resetScanningState()
            AttendanceCheckInUiEvent.NavigateBack -> viewModelScope.launch { _uiEffectFlow.send(AttendanceCaptureUiEffect.NavigateBack) }
        }
    }

    private fun resetScanningState() {
        _uiStateFlow.update { it.copy(error = null, studentProfile = null, isScanning = true, isLoading = false) }
        isProcessing.set(false)
    }

    private fun startScannerSession(email: String, mode: ScanMode) {
        val resolvedEmail = if (email.isBlank() || email == "admin@azuratech.com") {
            sessionManager.getAccountEmail().ifBlank { "admin@azuratech.com" }
        } else {
            email
        }
        currentAccountEmail = resolvedEmail
        _uiStateFlow.update { it.copy(isLoading = true, error = null, scanMode = mode) }

        viewModelScope.launch {
            val resolvedSchoolId = sessionManager.getActiveSchoolId()

            when (val sessionResult = repository.getSessionData(resolvedEmail, resolvedSchoolId)) {
                is com.azuratech.azuraengine.result.Result.Success -> {
                    val (classId, className, schoolId) = sessionResult.data
                    activeClassId = classId

                    if (schoolId != null) {
                        when (val galleryResult = repository.loadGallery(schoolId)) {
                            is com.azuratech.azuraengine.result.Result.Success -> {
                                gallery = galleryResult.data
                                _uiStateFlow.update {
                                    it.copy(
                                        isLoading = false,
                                        activeClassId = classId,
                                        activeClassName = className,
                                        activeSchoolId = schoolId,
                                        isScanning = true,
                                    )
                                }
                            }
                            is com.azuratech.azuraengine.result.Result.Failure -> {
                                _uiStateFlow.update { it.copy(isLoading = false, error = galleryResult.error.message, isScanning = false) }
                            }
                            else -> {}
                        }
                    } else {
                        _uiStateFlow.update { it.copy(isLoading = false, error = "Workspace Belum Dipilih", isScanning = false) }
                    }
                }
                is com.azuratech.azuraengine.result.Result.Failure -> {
                    _uiStateFlow.update { it.copy(isLoading = false, error = sessionResult.error.message, isScanning = false) }
                }
                else -> {}
            }
        }
    }

    private fun processScannedBiometric(embedding: FloatArray) {
        if (isProcessing.getAndSet(true)) return

        viewModelScope.launch {
            val schoolId = _uiStateFlow.value.activeSchoolId
            if (schoolId == null) {
                handleError("Error: Context Hilang")
                return@launch
            }

            _uiStateFlow.update { it.copy(isLoading = true, isScanning = false, error = null, studentProfile = null) }

            when (val matchResult = repository.performMatch(embedding, gallery)) {
                is com.azuratech.azuraengine.result.Result.Success -> {
                    val matchedStudentId = matchResult.data
                    if (matchedStudentId != null) {
                        processAttendanceRecord(matchedStudentId, schoolId)
                    } else {
                        handleUnregistered()
                    }
                }
                is com.azuratech.azuraengine.result.Result.Failure -> {
                    handleError(matchResult.error.message ?: "Match Error")
                }
                else -> {}
            }
        }
    }

    private fun processScannedBarcode(barcode: String) {
        if (isProcessing.getAndSet(true)) return

        viewModelScope.launch {
            val currentSchoolId = _uiStateFlow.value.activeSchoolId
            if (currentSchoolId == null) {
                handleError("Error: Context Hilang")
                return@launch
            }

            // 🔥 AI Native: Smart Barcode Parsing
            // Support format: schoolId|classId|studentId
            val parts = barcode.split("|")
            val (scannedSchoolId, scannedClassId, scannedStudentId) = when (parts.size) {
                3 -> Triple(parts[0], parts[1], parts[2])
                2 -> Triple(parts[0], null, parts[1]) // fallback for schoolId|studentId
                else -> Triple(currentSchoolId, null, barcode) // legacy studentId only
            }

            // 1. Validate School Context
            if (scannedSchoolId != currentSchoolId) {
                handleError("Barcode Sekolah Lain!")
                return@launch
            }

            // 2. Resolve Class Context (Optional but improves check-in accuracy)
            if (scannedClassId != null && scannedClassId != "UNASSIGNED" && _uiStateFlow.value.activeClassId != null) {
                if (scannedClassId != _uiStateFlow.value.activeClassId) {
                    handleError("Bukan Kelas Ini!")
                    return@launch
                }
            }

            _uiStateFlow.update { it.copy(isLoading = true, isScanning = false, error = null, studentProfile = null, scannedResult = scannedStudentId) }
            processAttendanceRecord(scannedStudentId, currentSchoolId)
        }
    }

    private suspend fun processAttendanceRecord(scannedId: String, schoolId: String) {
        val currentTime = System.currentTimeMillis()
        if (scannedId == lastProcessedStudentId && (currentTime - lastProcessedTime < REPEAT_SCAN_SUPPRESSION_MS)) {
            // Deduplication logic
            isProcessing.set(false)
            _uiStateFlow.update { it.copy(isLoading = false, isScanning = true) }
            return
        }

        val biometricResult = attendanceRepository.getStudentBiometricById(scannedId, schoolId)
        val studentBiometric = (biometricResult as? Result.Success)?.data
        if (studentBiometric == null) {
            handleUnregistered()
            return
        }

        val classIdsResult = attendanceRepository.getClassIdsForStudentFlow(scannedId, schoolId).firstOrNull() ?: Result.Success(emptyList())
        val studentClassIds = if (classIdsResult is Result.Success) classIdsResult.data else emptyList()

        val params = ProcessAttendanceParams(
            studentId = scannedId,
            studentName = studentBiometric.name,
            accountEmail = currentAccountEmail,
            activeClassId = _uiStateFlow.value.activeClassId,
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
                        handleCheckInSuccess(scannedId, attendanceRes.name, "${attendanceRes.name}, already checked in.", true)
                    }
                    is AttendanceResult.Rejected -> {
                        handleError("${attendanceRes.name}: Not in this class!")
                    }
                    AttendanceResult.Unregistered -> handleUnregistered()
                }
            }
            is Result.Failure -> {
                handleError(result.error.message ?: "Check-in Failed")
            }
            is Result.Loading -> { /* Not used here */ }
        }
    }

    private suspend fun handleCheckInSuccess(studentId: String, name: String, speakMessage: String, alreadyCheckedIn: Boolean) {
        lastProcessedStudentId = studentId
        lastProcessedTime = System.currentTimeMillis()
        _uiStateFlow.update {
            it.copy(
                isLoading = false,
                studentProfile = StudentProfile(studentId = studentId, name = name, schoolId = _uiStateFlow.value.activeSchoolId ?: ""),
                isAlreadyCheckedIn = alreadyCheckedIn,
            )
        }
        _uiEffectFlow.send(AttendanceCaptureUiEffect.Speak(speakMessage))
        enterCooldown()
    }

    private suspend fun handleUnregistered() {
        handleError("Unknown Identity")
    }

    private suspend fun handleError(message: String) {
        _uiStateFlow.update { it.copy(isLoading = false, error = message, isScanning = false) }
        _uiEffectFlow.send(AttendanceCaptureUiEffect.Speak(message))
        enterCooldown()
    }

    private suspend fun enterCooldown(duration: Long = 4000) {
        delay(duration)
        resetScanningState()
    }
}
