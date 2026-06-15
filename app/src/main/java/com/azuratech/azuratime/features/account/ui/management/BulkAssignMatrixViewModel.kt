package com.azuratech.azuratime.features.account.ui.management

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.domain.usecase.ImportMatrixUseCase
import com.azuratech.azuratime.features.account.domain.usecase.MatrixImportPreview
import com.azuratech.azuraengine.core.StorageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class BulkAssignMatrixViewModel @Inject constructor(
    private val importUseCase: ImportMatrixUseCase,
    private val sessionManager: SessionManager,
    private val storageProvider: StorageProvider,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(BulkAssignMatrixUiState())
    val uiStateFlow: StateFlow<BulkAssignMatrixUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<BulkAssignMatrixUiEffect>()
    val uiEffectFlow: SharedFlow<BulkAssignMatrixUiEffect> = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: BulkAssignMatrixUiEvent) {
        when (event) {
            is BulkAssignMatrixUiEvent.ProcessFile -> processFile(event.uri)
            BulkAssignMatrixUiEvent.Commit -> commit()
            BulkAssignMatrixUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun processFile(uri: Uri) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, error = null, previews = emptyList()) }

            try {
                val bytes = storageProvider.read(uri.toString())
                if (bytes.isEmpty()) {
                    _uiStateFlow.update { it.copy(isLoading = false, error = "File is empty") }
                    return@launch
                }

                val rows = parseCsv(bytes)
                val schoolId = sessionManager.getActiveSchoolId() ?: ""

                val previews = importUseCase.resolveRows(schoolId, rows)
                _uiStateFlow.update { it.copy(isLoading = false, previews = previews) }
            } catch (e: Exception) {
                _uiStateFlow.update { it.copy(isLoading = false, error = e.message ?: "Failed to parse file") }
            }
        }
    }

    private fun parseCsv(bytes: ByteArray): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val headers = parseCsvLine(headerLine).map { it.lowercase().replace(" ", "").replace("_", "") }

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val columns = parseCsvLine(line!!)
                val row = mutableMapOf<String, String>()
                headers.forEachIndexed { index, header ->
                    if (index < columns.size) {
                        row[header] = columns[index]
                    }
                }
                // Map to canonical keys expected by use case
                val mappedRow = mutableMapOf<String, String>()
                mappedRow["teacher_email"] = row["teacheremail"] ?: ""
                mappedRow["class_name"] = row["classname"] ?: ""
                mappedRow["subject_name"] = row["subjectname"] ?: ""
                result.add(mappedRow)
            }
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when (char) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim().removeSurrounding("\""))
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString().trim().removeSurrounding("\""))
        return result
    }

    private fun commit() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            _uiStateFlow.update { it.copy(isCommitting = true) }

            importUseCase.commit(schoolId, _uiStateFlow.value.previews)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isCommitting = false, success = true) }
                    _uiEffectFlow.emit(BulkAssignMatrixUiEffect.ShowToast("Successfully updated assignments"))
                    _uiEffectFlow.emit(BulkAssignMatrixUiEffect.NavigateBack)
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isCommitting = false, error = error.message) }
                }
        }
    }
}

data class BulkAssignMatrixUiState(
    val isLoading: Boolean = false,
    val isCommitting: Boolean = false,
    val previews: List<MatrixImportPreview> = emptyList(),
    val error: String? = null,
    val success: Boolean = false,
)

sealed class BulkAssignMatrixUiEvent {
    data class ProcessFile(val uri: Uri) : BulkAssignMatrixUiEvent()
    object Commit : BulkAssignMatrixUiEvent()
    object ClearError : BulkAssignMatrixUiEvent()
}

sealed class BulkAssignMatrixUiEffect {
    data class ShowToast(val message: String) : BulkAssignMatrixUiEffect()
    object NavigateBack : BulkAssignMatrixUiEffect()
}
