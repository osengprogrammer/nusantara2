package com.azuratech.azuratime.core.boot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.repo.BootRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel // 🔥 1. Tandai sebagai ViewModel Hilt
class BootViewModel @Inject constructor( // 🔥 2. Inject BootRepository
    application: Application,
    private val repository: BootRepository // 🔥 Disuplai otomatis oleh Hilt
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
                    val currentUser = repository.getCurrentUser()
                    
                    withContext(Dispatchers.Main) {
                        val isLoggedIn = currentUser != null
                        
                        if (!isLoggedIn) {
                            _stateFlow.value = BootState.NeedLogin
                        } else {
                            val isActive = repository.isSessionActive() 
                            _stateFlow.value = if (isActive) BootState.Ready else BootState.NeedActivation
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