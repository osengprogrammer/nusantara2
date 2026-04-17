package com.azuratech.azuratime.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZoharAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val checkInRecordDao by lazy { AppDatabase.Companion.getInstance(application).checkInRecordDao() }

    // 🔥 Added SessionManager to get schoolId
    private val schoolId: String get() = SessionManager.Companion.getInstance(getApplication()).getActiveSchoolId() ?: ""

    private val zoharBrain = ZoharBrain(apiKey = BuildConfig.GEMINI_API_KEY)

    private val _zoharResponse =
        MutableStateFlow("Halo Brother! Zohar siap mengawal Azura Ecosystem. Ada yang bisa Zohar bantu? Joss Gandos!")
    val zoharResponse: StateFlow<String> = _zoharResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun askZohar(userQuestion: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // 🔥 FIXED: Passed schoolId to Zohar's memory fetch!
                val recentLogs = checkInRecordDao.getAllRecords(schoolId).first().take(10)
                val contextData = if (recentLogs.isEmpty()) "Belum ada data absensi."
                                 else recentLogs.joinToString("\n") {
                                     "${it.name} status ${it.status} pada ${it.attendanceDate}"
                                 }

                val fullPrompt = """
                    Data Absensi Terbaru:
                    $contextData
                    
                    Pertanyaan Owner:
                    $userQuestion
                """.trimIndent()

                _zoharResponse.value = zoharBrain.think(fullPrompt)
            } catch (e: Exception) {
                _zoharResponse.value = "Zohar mengalami gangguan koneksi: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}