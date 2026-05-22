package com.azuratech.azuratime.features.update.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.features.update.domain.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 🚀 APP UPDATE VIEW MODEL (v3.2.0-ai-native)
 * Strictly follows MVI pattern with Effect-Driven side effects.
 */
@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
    private val eventBus: UpdateEventBus,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(AppUpdateUiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<AppUpdateUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            eventBus.events.collect {
                checkForUpdate()
            }
        }
    }

    fun onEvent(event: AppUpdateUiEvent) {
        when (event) {
            is AppUpdateUiEvent.CheckForUpdate -> checkForUpdate()
            is AppUpdateUiEvent.DownloadUpdate -> downloadUpdate()
            is AppUpdateUiEvent.InstallUpdate -> installUpdate()
            is AppUpdateUiEvent.DismissDialog -> _uiStateFlow.update { it.copy(showDialog = false) }
            is AppUpdateUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.checkForUpdate()
                .onSuccess { info ->
                    val currentVersionCode = BuildConfig.VERSION_CODE
                    if (info.versionCode > currentVersionCode) {
                        _uiStateFlow.update {
                            it.copy(
                                isLoading = false,
                                updateAvailable = true,
                                releaseNotes = info.releaseNotes,
                                downloadUrl = info.downloadUrl,
                                showDialog = true,
                            )
                        }
                    } else {
                        _uiStateFlow.update { it.copy(isLoading = false, updateAvailable = false) }
                    }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun downloadUpdate() {
        val url = _uiStateFlow.value.downloadUrl
        if (url.isBlank()) return

        viewModelScope.launch {
            // Use external files dir to avoid needing MANAGE_EXTERNAL_STORAGE on modern Android for simple APK download
            // Installation will still need REQUEST_INSTALL_PACKAGES
            val targetFile = File(context.getExternalFilesDir(null), "update.apk")
            repository.downloadApk(url, targetFile).collect { result ->
                result.onSuccess { progress ->
                    _uiStateFlow.update { it.copy(downloadProgress = progress) }
                    if (progress >= 1.0f) {
                        _uiStateFlow.update { it.copy(apkFile = targetFile) }
                        _uiEffectFlow.emit(AppUpdateUiEffect.ShowToast("Unduhan selesai. Siap dipasang."))
                    }
                }
                    .onFailure { error ->
                        _uiStateFlow.update { it.copy(error = error.message) }
                    }
            }
        }
    }

    private fun installUpdate() {
        val file = _uiStateFlow.value.apkFile
        if (file != null && file.exists()) {
            viewModelScope.launch {
                _uiEffectFlow.emit(AppUpdateUiEffect.InstallApk(file))
            }
        } else {
            viewModelScope.launch {
                _uiEffectFlow.emit(AppUpdateUiEffect.ShowToast("Berkas APK tidak ditemukan"))
            }
        }
    }
}
