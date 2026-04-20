package com.azuratech.azuratime.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repo.WorkspaceRepository
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.azuratech.azuratime.domain.user.usecase.SyncUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 🛠️ WORKSPACE VIEW MODEL
 * Mengelola perpindahan antar sekolah, pencarian sekolah, dan pembuatan workspace baru.
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
 */
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    application: Application,
    private val repository: WorkspaceRepository,
    private val userRepository: UserRepository,
    private val syncUserUseCase: SyncUserUseCase,
    private val sessionManager: SessionManager,
    private val db: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class WorkspaceState {
        object Idle : WorkspaceState()
        object Switching : WorkspaceState()
        data class Success(val schoolName: String) : WorkspaceState()
        data class Error(val message: String) : WorkspaceState()
    }

    private val _uiState = MutableStateFlow<WorkspaceState>(WorkspaceState.Idle)
    val uiState: StateFlow<WorkspaceState> = _uiState.asStateFlow()

    /**
     * Berpindah workspace sekolah aktif.
     * Menghapus data tenant lama dan mengunduh data tenant baru (Faces, Classes, Records).
     */
    fun changeWorkspace(userId: String, newSchoolId: String, newSchoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                // 1. Update SessionManager agar DAO lain langsung tahu sekolah mana yang aktif
                sessionManager.saveActiveSchoolId(newSchoolId)

                // 2. Update Cloud agar user kembali ke sekolah ini saat login nanti
                // Logic moved to WorkspaceRepository.switchWorkspace
                repository.switchWorkspace(userId, newSchoolId)

                // 3. Sync User agar Role/Membership terbaru masuk ke Room
                syncUserUseCase(userId)

                _uiState.value = WorkspaceState.Success(newSchoolName)
            } catch (e: Exception) {
                _uiState.value = WorkspaceState.Error("Gagal pindah workspace: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = WorkspaceState.Idle
    }

    // =====================================================
    // 🔍 SCHOOL DISCOVERY & JOIN
    // =====================================================
    private val _schoolSearchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val schoolSearchResults: StateFlow<List<Map<String, Any>>> = _schoolSearchResults.asStateFlow()

    fun searchSchools(query: String) {
        if (query.length < 3) {
            _schoolSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
                val snapshot = db.collection("schools")
                    .whereGreaterThanOrEqualTo("schoolName", query)
                    .whereLessThanOrEqualTo("schoolName", query + "\uf8ff")
                    .get()
                _schoolSearchResults.value = snapshot.await().documents.mapNotNull { it.data }
        }
    }

    fun sendJoinRequest(user: UserEntity, schoolId: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                db.collection("whitelisted_users").document(user.userId)
                    .update(
                        mapOf(
                            "memberships.$schoolId.schoolName" to schoolName,
                            "memberships.$schoolId.role" to "PENDING"
                        )
                    ).await()
                _uiState.value = WorkspaceState.Success(schoolName)
            } catch (e: Exception) {
                _uiState.value = WorkspaceState.Error(e.message ?: "Gagal mengirim permintaan")
            }
        }
    }

    // =====================================================
    // 🏗️ CREATION & SETUP
    // =====================================================

    fun createNewSchool(userId: String, userEmail: String, schoolName: String) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                // 1. Buat workspace di Firestore
                val newSchoolRef = db.collection("schools").document()
                val schoolId = newSchoolRef.id

                newSchoolRef.set(mapOf(
                    "schoolId" to schoolId,
                    "schoolName" to schoolName,
                    "ownerId" to userId,
                    "ownerEmail" to userEmail,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )).await()

                // 2. Kunci sesi ke tenant baru ini
                sessionManager.saveActiveSchoolId(schoolId)

                // 3. Tarik membership terbaru (sebagai ADMIN sekolah baru)
                syncUserUseCase(userId)

                _uiState.value = WorkspaceState.Success(schoolName)
            } catch (e: Exception) {
                _uiState.value = WorkspaceState.Error("Gagal membuat sekolah: ${e.message}")
            }
        }
    }

    fun finalizeSetup(schoolId: String) {
        viewModelScope.launch {
            try {
                db.collection("schools").document(schoolId).update("status", "ACTIVE").await()
            } catch (e: Exception) {
                // Non-critical
            }
        }
    }

    fun updateSchoolName(schoolId: String, userId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = WorkspaceState.Switching
            try {
                db.collection("schools").document(schoolId).update("schoolName", newName.trim()).await()
                syncUserUseCase(userId)
                _uiState.value = WorkspaceState.Idle
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = WorkspaceState.Error("Gagal mengubah nama sekolah: ${e.message}")
                onError(e.message ?: "Gagal mengubah nama sekolah")
            }
        }
    }
}
// 🔥 FACTORY DIHAPUS SEPENUHNYA