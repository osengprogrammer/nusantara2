package com.azuratech.azuratime.features.attendance.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.attendance.data.repo.BiometricScannerRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
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
    private val repository: BiometricScannerRepository,
    private val attendanceRepository: AttendanceRepository,
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

    // Tracking for deduplication to prevent "double output"
    private var lastProcessedStudentId: String? = null
    private var lastProcessedTime: Long = 0L
    private val REPEAT_SCAN_SUPPRESSION_MS = 600_000L // 10 minutes (60s * 10)

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

    fun processScannedBiometric(embedding: FloatArray) {
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
            val matchedStudentId = repository.performMatch(embedding, gallery)

            if (matchedStudentId != null) {
                processAttendanceRecord(matchedStudentId)
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

        // 🔥 Deduplication logic to prevent "double output"
        val currentTime = System.currentTimeMillis()
        if (scannedId == lastProcessedStudentId && (currentTime - lastProcessedTime < REPEAT_SCAN_SUPPRESSION_MS)) {
            // Student just scanned successfully, ignore this frame to prevent repeat sound/UI
            isProcessing.set(false)
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
            studentClassIds = studentClassIds
        )

        val result = attendanceRepository.processAttendance(params)

        when (result) {
            is Result.Success<AttendanceResult> -> {
                when (val attendanceRes = result.data) {
                    is AttendanceResult.Success -> {
                        lastProcessedStudentId = scannedId
                        lastProcessedTime = System.currentTimeMillis()
                        _uiState.value = AttendanceUiState.Success(attendanceRes.name, alreadyCheckedIn = false)
                        _sideEffect.send(AttendanceSideEffect.Speak(attendanceRes.message))
                    }
                    is AttendanceResult.AlreadyCheckedIn -> {
                        // Even if already checked in, we update the timestamp to prevent 
                        // "Already checked in" from firing repeatedly every 4s if they stand there
                        lastProcessedStudentId = scannedId
                        lastProcessedTime = System.currentTimeMillis()

                        _uiState.value = AttendanceUiState.Success(attendanceRes.name, alreadyCheckedIn = true)
                        _sideEffect.send(AttendanceSideEffect.Speak("${attendanceRes.name}, sudah absen."))
                    }
                    is AttendanceResult.Rejected -> {
                        _uiState.value = AttendanceUiState.Error("${attendanceRes.name}: Bukan Kelas Ini!")
                        _sideEffect.send(AttendanceSideEffect.Speak("${attendanceRes.name}: Bukan Kelas Ini!"))
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

    private suspend fun enterCooldown(duration: Long = 4000) {
        delay(duration)
        _uiState.value = AttendanceUiState.Idle
        isProcessing.set(false)
    }
}
