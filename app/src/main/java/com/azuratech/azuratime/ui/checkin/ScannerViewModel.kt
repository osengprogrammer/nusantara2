package com.azuratech.azuratime.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repo.CheckInResult
import com.azuratech.azuratime.data.repo.ScannerRepository
import com.azuratech.azuratime.domain.school.usecase.GetActiveSchoolContextUseCase
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
    private val getActiveSchoolContextUseCase: GetActiveSchoolContextUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<CheckInSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

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
            // 1. Resolve Context via UseCase (P0 Fix)
            val contextResult = getActiveSchoolContextUseCase()
            val resolvedSchoolId = if (contextResult is Result.Success) contextResult.data.schoolId else null
            
            // 2. Fetch Session Data
            val (classId, className, schoolId) = repository.getSessionData(email, resolvedSchoolId)
            activeClassId = classId
            activeClassName = className
            activeSchoolId = schoolId
            
            if (schoolId != null) {
                gallery = repository.loadGallery(schoolId)
            } else {
                _uiState.value = CheckInUiState.Error("Workspace Belum Dipilih")
            }
        }
    }

    fun processScannedFace(embedding: FloatArray) {
        // 1. Gatekeeper Logic: Ignore if already processing
        if (isProcessing.getAndSet(true)) {
            return
        }

        viewModelScope.launch {
            if (activeSchoolId == null) {
                _uiState.value = CheckInUiState.Error("Error: Context Hilang")
                enterCooldown()
                return@launch
            }

            _uiState.value = CheckInUiState.Processing
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
                _uiState.value = CheckInUiState.Error("Error: Context Hilang")
                enterCooldown()
                return@launch
            }
            _uiState.value = CheckInUiState.Processing
            processAttendanceRecord(barcode)
        }
    }

    private suspend fun processAttendanceRecord(scannedId: String) {
        val result = repository.processCheckIn(
            faceId = scannedId,
            teacherEmail = currentTeacherEmail,
            classId = activeClassId,
            className = activeClassName,
            schoolId = activeSchoolId ?: ""
        )

        when (result) {
            is CheckInResult.Success -> {
                _uiState.value = CheckInUiState.Success(result.name, alreadyCheckedIn = false)
                _sideEffect.send(CheckInSideEffect.Speak("Selamat datang, ${result.name}"))
            }
            is CheckInResult.AlreadyCheckedIn -> {
                _uiState.value = CheckInUiState.Success(result.name, alreadyCheckedIn = true)
                _sideEffect.send(CheckInSideEffect.Speak("${result.name}, sudah absen."))
            }
            is CheckInResult.Rejected -> {
                _uiState.value = CheckInUiState.Error("${result.name}: Bukan Kelas Ini!")
                _sideEffect.send(CheckInSideEffect.Speak("${result.name}: Bukan Kelas Ini!"))
            }
            is CheckInResult.Unregistered -> {
                handleUnregistered()
            }
        }
        // 2. Cooldown Logic
        enterCooldown()
    }

    private suspend fun handleUnregistered() {
        _uiState.value = CheckInUiState.Error("Wajah Tidak Dikenal")
        _sideEffect.send(CheckInSideEffect.Speak("Wajah Tidak Dikenal"))
        enterCooldown()
    }

    private suspend fun enterCooldown(duration: Long = 2500) {
        delay(duration)
        _uiState.value = CheckInUiState.Idle
        isProcessing.set(false) // Open the gate
    }
}
