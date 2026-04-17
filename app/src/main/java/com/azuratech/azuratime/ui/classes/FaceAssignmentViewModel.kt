package com.azuratech.azuratime.ui.classes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.repository.FaceAssignmentRepository
import com.azuratech.azuratime.data.repository.ClassRepository
import com.azuratech.azuratime.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel // 🔥 Tambahan Import
import javax.inject.Inject // 🔥 Tambahan Import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 🛠️ FACE ASSIGNMENT VIEW MODEL
 * Menjembatani UI dengan Repository untuk pendaftaran siswa ke kelas.
 * 🔥 Full Hilt Architecture & Multi-Tenant Ready.
 */
@HiltViewModel // 🔥 1. Tandai sebagai Hilt ViewModel
class FaceAssignmentViewModel @Inject constructor( // 🔥 2. Inject semua dependensi
    application: Application,
    database: AppDatabase,
    private val repository: FaceAssignmentRepository,
    private val classRepository: ClassRepository,
    private val sessionManager: SessionManager // 🔥 SessionManager disuntikkan langsung
) : AndroidViewModel(application) {

    // ❌ Inisialisasi manual Repository & FirestoreManager DIHAPUS
    
    private val faceDao = database.faceDao()
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    /**
     * 🔥 THE POWER MAP: faceId -> List<ClassEntity>
     * Mengalirkan data pemetaan siswa ke kelas secara real-time.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allAssignedClassesMap: StateFlow<Map<String, List<ClassEntity>>> =
        faceDao.getAllFacesFlow(schoolId)
            .flatMapLatest { faces ->
                if (faces.isEmpty()) return@flatMapLatest flowOf(emptyMap())
                val flows = faces.map { face ->
                    repository.getAssignedClassesDetails(face.faceId)
                        .map { classes -> face.faceId to classes }
                }
                combine(flows) { pairs -> pairs.toMap() }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // =====================================================
    // 📊 MASTER CLASS LIST
    // =====================================================

    // Menyediakan daftar semua kelas yang tersedia di sekolah ini
    val availableClasses: Flow<List<ClassEntity>> = classRepository.allClasses

    // =====================================================
    // 🔗 ASSIGNMENT OPERATIONS
    // =====================================================

    fun getAssignedClassIds(faceId: String): Flow<List<String>> =
        repository.getAssignedClassIds(faceId)

    fun assignToClass(faceId: String, classId: String) {
        viewModelScope.launch {
            repository.assignToClass(faceId, classId)
        }
    }

    fun removeSpecificAssignment(faceId: String, classId: String) {
        viewModelScope.launch {
            repository.removeSpecificAssignment(faceId, classId)
        }
    }

    fun removeAllAssignmentsForFace(faceId: String) {
        viewModelScope.launch {
            repository.removeAllAssignmentsForFace(faceId)
        }
    }
}

// ❌ CLASS FACEASSIGNMENTVIEWMODELFACTORY DIHAPUS SEPENUHNYA
// Hilt sudah menangani pembuatan ViewModel ini secara otomatis.