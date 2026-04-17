package com.azuratech.azuratime.ui.classes

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.repository.ClassRepository
import dagger.hilt.android.lifecycle.HiltViewModel // 🔥 Tambahan Import
import javax.inject.Inject // 🔥 Tambahan Import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🛠️ CLASS VIEW MODEL - PURE & CLEAN
 * Menggunakan Hilt untuk manajemen dependensi dan SessionManager untuk identitas tenant.
 */
@HiltViewModel // 🔥 1. Wajib untuk Hilt ViewModel
class ClassViewModel @Inject constructor( // 🔥 2. Inject dependensi via konstruktor
    application: Application,
    private val repository: ClassRepository // 🔥 Disuplai otomatis oleh Hilt
) : AndroidViewModel(application) {

    // ❌ Inisialisasi manual repository & sessionManager DIHAPUS

    // =====================================================
    // 📊 CLASS FLOWS (State Management)
    // =====================================================

    /**
     * Mengamati daftar kelas secara reaktif melalui Repository.
     * Secara otomatis berubah jika activeSchoolId di SessionManager berganti.
     */
    val classes: StateFlow<List<ClassEntity>> = repository.allClasses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // =====================================================
    // ➕ CRUD OPERATIONS
    // =====================================================

    fun addClass(name: String) {
        viewModelScope.launch {
            // Menggunakan upsertClass sesuai yang ada di Repository terbaru
            repository.upsertClass(name)
        }
    }

    fun updateClass(classId: String, newName: String) {
        viewModelScope.launch {
            // Menggunakan upsertClass dengan ID untuk proses update
            repository.upsertClass(newName, classId)
        }
    }

    fun deleteClass(
        classEntity: ClassEntity,
        onFailure: (String) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.deleteClass(classEntity.id)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { onFailure(it.message ?: "Gagal menghapus kelas") }
                )
            }
        }
    }

    // =====================================================
    // 📥 IMPORT CSV LOGIC
    // =====================================================

    suspend fun analyzeImport(csvData: List<Map<String, String>>): List<ImportAnalysis> = 
        withContext(Dispatchers.IO) {
            val existingClasses = classes.value
            
            csvData.map { row ->
                val name = row["name"] ?: ""
                if (name.isEmpty()) {
                    ImportAnalysis(row, ImportStatus.ERROR)
                } else {
                    val exists = existingClasses.any { it.name.equals(name, ignoreCase = true) }
                    ImportAnalysis(row, if (exists) ImportStatus.UPDATE else ImportStatus.NEW)
                }
            }
        }

    fun importClassesFromCsv(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Logika import panggil repository.bulkImportClasses jika datanya sudah siap
            withContext(Dispatchers.Main) { onComplete() }
        }
    }
}

// Data structures needed for Import functionality
enum class ImportStatus {
    NEW, UPDATE, ERROR
}

data class ImportAnalysis(
    val rowData: Map<String, String>,
    val status: ImportStatus
)