package com.azuratech.azuratime.ui.add

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repository.RegistrationRepository
import com.azuratech.azuratime.domain.model.ProcessResult
import com.azuratech.azuratime.domain.sync.usecase.ProcessCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val results: List<ProcessResult> = emptyList()
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    application: Application,
    private val repository: RegistrationRepository,
    private val processCsvUseCase: ProcessCsvUseCase
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun resetState() {
        _state.value = RegisterState()
    }

    // =====================================================
    // 🚀 MANTRA PEMPROSES CSV BATCH (BULK IMPORT)
    // =====================================================
    fun processCsvFile(uri: Uri, dataType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = RegisterState(isProcessing = true, status = "Mempersiapkan data...", progress = 0.05f)
            val resultLogs = mutableListOf<ProcessResult>()

            try {
                processCsvUseCase(uri, dataType).collect { result ->
                    if (result.status != "Syncing") {
                        resultLogs.add(result)
                    }

                    val currentProgress = if (resultLogs.isEmpty()) 0.1f else minOf(0.95f, (resultLogs.size * 0.05f))

                    _state.value = _state.value.copy(
                        progress = currentProgress,
                        status = "Memproses: ${result.name} (${result.status})",
                        results = resultLogs.toList()
                    )
                }

                val successCount = resultLogs.count { it.status == "Registered" || it.status.contains("Updated") }
                _state.value = RegisterState(
                    isProcessing = false, 
                    progress = 1.0f,
                    status = "Import Selesai! $successCount siswa berhasil diproses.", 
                    results = resultLogs
                )

            } catch (e: Exception) {
                Log.e("AZURA_BULK", "Error memproses CSV: ${e.message}", e)
                _state.value = RegisterState(
                    isProcessing = false, 
                    status = "Terjadi Kesalahan Kritis: ${e.message}", 
                    results = resultLogs
                )
            }
        }
    }

    // Stub — Export & Download logic
    fun exportMasterData(context: Context, dataType: String) {}
    fun downloadCsvTemplate(context: Context, dataType: String) {}
    fun getCsvRowCount(): Int = 0
}