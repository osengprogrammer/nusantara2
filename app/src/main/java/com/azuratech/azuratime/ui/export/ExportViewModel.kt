package com.azuratech.azuratime.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.toProfile
import com.azuratech.azuratime.data.repo.ExportRepository
import com.azuratech.azuratime.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ EXPORT VIEW MODEL - Phase 7.11 SSOT Migration
 * Observes ExportJobProfile stream directly from Room.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val exportJobs: StateFlow<List<ExportJobProfile>> = 
        sessionManager.currentUserIdFlow.filterNotNull()
            .flatMapLatest { userId -> 
                exportRepository.observeExportJobsByUser(userId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startExport(fileType: String) {
        viewModelScope.launch {
            val result = exportRepository.createExportJob(fileType)
            if (result is Result.Success) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Export job started"))
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal: ${result.error.message}"))
            }
        }
    }
}
