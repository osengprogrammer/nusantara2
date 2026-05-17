package com.azuratech.azuratime.features.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZoharAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceRecordDao by lazy { AppDatabase.Companion.getInstance(application).attendanceRecordDao() }

    // 🔥 Added SessionManager to get schoolId
    private val schoolId: String get() = SessionManager.Companion.getInstance(getApplication()).getActiveSchoolId() ?: ""

    private val zoharBrain = ZoharBrain(apiKey = BuildConfig.GEMINI_API_KEY)

    private val _zoharResponseFlow =
        MutableStateFlow("Halo Brother! Zohar siap mengawal Azura Ecosystem. Ada yang bisa Zohar bantu? Joss Gandos!")
    val zoharResponseFlow: StateFlow<String> = _zoharResponseFlow

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow

    fun askZohar(userQuestion: String) {
        viewModelScope.launch {
            _isLoadingFlow.value = true

            try {
                // 🔥 FIXED: Passed schoolId to Zohar's memory fetch!
                val recentLogs = attendanceRecordDao.getAllRecords(schoolId).first().take(10)
                val contextData = if (recentLogs.isEmpty()) {
                    "Belum ada data absensi."
                } else {
                    recentLogs.joinToString("\n") {
                        "${it.name} status ${it.status} pada ${it.attendanceDate}"
                    }
                }

                val fullPrompt = """
                    Data Absensi Terbaru:
                    $contextData
                    
                    Pertanyaan Owner:
                    $userQuestion
                """.trimIndent()

                _zoharResponseFlow.value = zoharBrain.think(fullPrompt)
            } catch (e: Exception) {
                _zoharResponseFlow.value = "Zohar mengalami gangguan koneksi: ${e.message}"
            } finally {
                _isLoadingFlow.value = false
            }
        }
    }
}
