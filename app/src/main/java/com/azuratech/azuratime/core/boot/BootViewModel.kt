package com.azuratech.azuratime.core.boot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.domain.repository.BootRepository
import dagger.hilt.android.lifecycle.HiltViewModel // 🔥 Tambahan Import
import javax.inject.Inject // 🔥 Tambahan Import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@HiltViewModel // 🔥 1. Tandai sebagai ViewModel Hilt
class BootViewModel @Inject constructor( // 🔥 2. Inject BootRepository
    application: Application,
    private val repository: BootRepository, // 🔥 Disuplai otomatis oleh Hilt
) : AndroidViewModel(application) {

    // ❌ HAPUS inisialisasi manual repository lama

    private val _stateFlow = MutableStateFlow<BootState>(BootState.Loading)
    val stateFlow: StateFlow<BootState> = _stateFlow.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            _stateFlow.value = BootState.Loading

            withContext(Dispatchers.IO) {
                try {
                    delay(600) // Jeda untuk stabilitas pembacaan enkripsi
                    val accountResult = repository.getCurrentAccount()

                    withContext(Dispatchers.Main) {
                        val isLoggedIn = accountResult is com.azuratech.azuraengine.result.Result.Success && accountResult.data != null

                        if (!isLoggedIn) {
                            _stateFlow.value = BootState.NeedLogin
                        } else {
                            viewModelScope.launch {
                                when (val result = repository.isSessionActive()) {
                                    is com.azuratech.azuraengine.result.Result.Success -> {
                                        _stateFlow.value = if (result.data) BootState.Ready else BootState.NeedActivation
                                    }
                                    else -> {
                                        _stateFlow.value = BootState.Error("Gagal memuat sesi")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _stateFlow.value = BootState.Error("Gagal memuat sesi")
                    }
                }
            }
        }
    }

    fun recheck() {
        checkAuthStatus()
    }
}
