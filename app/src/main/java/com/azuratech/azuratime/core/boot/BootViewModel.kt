package com.azuratech.azuratime.core.boot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.repository.BootRepository
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
    private val repository: BootRepository // 🔥 Disuplai otomatis oleh Hilt
) : AndroidViewModel(application) {
    
    // ❌ HAPUS inisialisasi manual repository lama

    private val _state = MutableStateFlow<BootState>(BootState.Loading)
    val state: StateFlow<BootState> = _state.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        viewModelScope.launch { 
            _state.value = BootState.Loading
            
            withContext(Dispatchers.IO) {
                try {
                    delay(600) // Jeda untuk stabilitas pembacaan enkripsi
                    val currentUser = repository.getCurrentUser()
                    
                    withContext(Dispatchers.Main) {
                        if (currentUser == null) {
                            _state.value = BootState.NeedLogin
                        } else {
                            val isActive = repository.isSessionActive() 
                            _state.value = if (isActive) BootState.Ready else BootState.NeedActivation
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _state.value = BootState.Error("Gagal memuat sesi")
                    }
                }
            }
        }
    }

    fun recheck() {
        checkAuthStatus()
    }
}