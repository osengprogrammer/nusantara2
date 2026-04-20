package com.azuratech.azuratime.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repo.CheckInResult
import com.azuratech.azuratime.data.repo.ScannerRepository
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
    private val repository: ScannerRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<CheckInSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

    private var gallery: List<Pair<String, FloatArray>> = emptyList()
    private var currentTeacherEmail: String = ""
    private var activeClassId: String? = null
    var activeClassName: String = ""

    // Gatekeeper: Prevents multiple concurrent processing
    private val isProcessing = AtomicBoolean(false)

    fun startScannerSession(email: String) {
        currentTeacherEmail = email
        viewModelScope.launch {
            val (classId, className) = repository.getSessionData(email)
            activeClassId = classId
            activeClassName = className
            gallery = repository.loadGallery()
        }
    }

    fun processScannedFace(embedding: FloatArray) {
        // 1. Gatekeeper Logic: Ignore if already processing
        if (isProcessing.getAndSet(true)) {
            return
        }

        viewModelScope.launch {
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
            _uiState.value = CheckInUiState.Processing
            processAttendanceRecord(barcode)
        }
    }

    private suspend fun processAttendanceRecord(scannedId: String) {
        val result = repository.processCheckIn(
            faceId = scannedId,
            teacherEmail = currentTeacherEmail,
            classId = activeClassId,
            className = activeClassName
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
