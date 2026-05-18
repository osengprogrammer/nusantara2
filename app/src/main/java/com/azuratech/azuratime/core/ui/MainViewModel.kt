package com.azuratech.azuratime.core.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.domain.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 🛠️ MAIN VIEW MODEL
 * Entry point aplikasi. Sangat bersih, mendelegasikan listener & AI ke MainRepository.
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
 */
@HiltViewModel
class MainViewModel @Inject constructor( // 🔥 FIX: Gunakan Hilt Inject
    application: Application,
    private val repository: MainRepository, // 🔥 FIX: Repositori disuntikkan secara otomatis
) : AndroidViewModel(application) {

    private val _isRevokedFlow = MutableStateFlow(false)
    val isRevokedFlow: StateFlow<Boolean> = _isRevokedFlow.asStateFlow()

    private var isInitialized = false
    private var revokeJob: Job? = null

    fun getCurrentEmail(): String = when (val result = repository.getCurrentEmail()) {
        is com.azuratech.azuraengine.result.Result.Success -> result.data
        else -> ""
    }

    fun initializeApp() {
        if (isInitialized) return
        isInitialized = true

        // 1. HIDUPKAN OTAK AI SECARA BACKGROUND
        viewModelScope.launch {
            repository.initializeAiBrain(getApplication())
        }

        // 2. JALANKAN SECURITY CLOUD (Background)
        val uidResult = repository.getCurrentUid()
        if (uidResult is com.azuratech.azuraengine.result.Result.Success && uidResult.data != null) {
            startRealtimeRevokeListener(uidResult.data!!)
        }
    }

    private fun startRealtimeRevokeListener(uid: String) {
        revokeJob?.cancel() // Bersihkan job lama jika ada

        revokeJob = viewModelScope.launch {
            repository.observeRevokeStatus(uid).collect { result ->
                if (result is com.azuratech.azuraengine.result.Result.Success && result.data) {
                    repository.executeRevocationCleanup()
                    _isRevokedFlow.value = true
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Job Flow otomatis berhenti saat ViewModel hancur, memory leak dicegah!
        revokeJob?.cancel()
    }
}
// 🔥 FIX: MainViewModelFactory DIHAPUS. Hilt sudah menanganinya.
