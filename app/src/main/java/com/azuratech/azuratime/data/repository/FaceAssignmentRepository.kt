package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 FACE ASSIGNMENT REPOSITORY
 * Hilt-ready Singleton.
 */
@Singleton
class FaceAssignmentRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceAssignmentDao = database.faceAssignmentDao()
    private val classDao = database.classDao()

    // 🔥 ID Sekolah dinamis dari SessionManager
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAssignedClassesMap: Flow<Map<String, List<ClassEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                combine(
                    faceAssignmentDao.getAllAssignments(schoolId),
                    classDao.observeClassesBySchool(schoolId)
                ) { assignments, classes ->
                    val classMap = classes.associateBy { it.id }
                    assignments.groupBy { it.faceId }
                        .mapValues { entry ->
                            entry.value.mapNotNull { classMap[it.classId] }
                        }
                }
            }

    // =====================================================
    // 📖 READ OPERATIONS
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAssignedClassIds(faceId: String): Flow<List<String>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                faceAssignmentDao.getClassIdsForFace(faceId, schoolId)
            }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAssignedClassesDetails(faceId: String): Flow<List<ClassEntity>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                faceAssignmentDao.getClassIdsForFace(faceId, schoolId).map { classIds ->
                    if (classIds.isEmpty()) return@map emptyList()

                    val classes = mutableListOf<ClassEntity>()
                    for (id in classIds) {
                        val classEntity = classDao.getClassById(id)
                        if (classEntity != null) {
                            classes.add(classEntity)
                        }
                    }
                    classes
                }
            }

    // =====================================================
    // ✍️ WRITE OPERATIONS & CLOUD SYNC
    // =====================================================

    suspend fun performAssignmentSync() = withContext(Dispatchers.IO) {
        if (schoolId.isEmpty()) return@withContext
        val assignments = mutableListOf<FaceAssignmentEntity>()

        // 1. Coba tarik dari Tenant Collection
        try {
            val snapshot = db.collection("schools").document(schoolId).collection("face_assignments").get().await()
            assignments.addAll(snapshot.documents.mapNotNull { doc ->
                var faceId = doc.getString("faceId")
                var classId = doc.getString("classId")
                
                // Auto-Healing: Jika field kosong, coba belah dari nama dokumen ("faceId_classId")
                if (faceId == null || classId == null) {
                    val parts = doc.id.split("_")
                    if (parts.size == 2) {
                        faceId = parts[0]
                        classId = parts[1]
                    }
                }
                
                val finalFaceId = faceId
                val finalClassId = classId
                
                if (finalFaceId != null && finalClassId != null) {
                    val correctedFaceId = if (finalFaceId.contains("--")) finalFaceId else "${finalClassId}--${finalFaceId}"
                    FaceAssignmentEntity(correctedFaceId, finalClassId, schoolId, true)
                } else null
            })
        } catch(e: Exception) { Log.e("AZURA_ASSIGN", "Gagal tarik tenant: ${e.message}") }

        // 2. FALLBACK/MIGRASI: Tarik dari Root Collection (Legacy Data)
        try {
            val rootSnapshot = db.collection("face_assignments").get().await()
            rootSnapshot.documents.forEach { doc ->
                var faceId = doc.getString("faceId")
                var classId = doc.getString("classId")
                
                // Auto-Healing
                if (faceId == null || classId == null) {
                    val parts = doc.id.split("_")
                    if (parts.size == 2) {
                        faceId = parts[0]
                        classId = parts[1]
                    }
                }
                
                val finalFaceId = faceId
                val finalClassId = classId
                
                if (finalFaceId != null && finalClassId != null) {
                    val correctedFaceId = if (finalFaceId.contains("--")) finalFaceId else "${finalClassId}--${finalFaceId}"
                    if (assignments.none { it.faceId == correctedFaceId && it.classId == finalClassId }) {
                        val legacyAssignment = FaceAssignmentEntity(correctedFaceId, finalClassId, schoolId, true)
                        assignments.add(legacyAssignment)
                        Log.i("AZURA_ASSIGN", "Found legacy assignment for Face: $correctedFaceId")
                    }
                }
            }
        } catch(e: Exception) { Log.e("AZURA_ASSIGN", "Gagal tarik root: ${e.message}") }

        // 3. Simpan ke Room
        Log.d("AZURA_TRUTH", "DATA DARI FIRESTORE (TOTAL): ${assignments.size}")
        
        // --- AUTO HEALING STRATEGY ---
        if (assignments.isEmpty()) {
            Log.i("AZURA_TRUTH", "⚠️ Cloud empty. Attempting Auto-Healing from Face IDs...")
            val allFacesList = database.faceDao().getAllFacesForScanningList(schoolId)
            for (face in allFacesList) {
                if (face.faceId.contains("--")) {
                    val parts = face.faceId.split("--")
                    if (parts.size >= 2) {
                        val reconstructedClassId = parts[0]
                        assignments.add(FaceAssignmentEntity(face.faceId, reconstructedClassId, schoolId, true))
                        Log.d("AZURA_TRUTH", "Auto-Healed: ${face.name} mapped to class $reconstructedClassId")
                    }
                }
            }
        }

        if (assignments.isNotEmpty()) {
            var successCount = 0
            assignments.forEach { 
                try {
                    faceAssignmentDao.insertAssignment(it) 
                    successCount++
                } catch (e: Exception) {
                    Log.e("AZURA_TRUTH", "FK ERROR: Gagal simpan assignment ${it.faceId} ke class ${it.classId}. Pastikan FaceId ada di tabel faces!")
                }
            }
            val countInRoom = faceAssignmentDao.getAssignmentCount(schoolId)
            Log.d("AZURA_TRUTH", "TOTAL ASSIGNMENTS DI ROOM: $countInRoom")
            Log.d("AZURA_ASSIGN", "✅ Berhasil menyimpan $successCount dari ${assignments.size} relasi ke Room!")
        } else {
            val countInRoom = faceAssignmentDao.getAssignmentCount(schoolId)
            Log.d("AZURA_TRUTH", "TOTAL ASSIGNMENTS DI ROOM: $countInRoom (Firestore Empty)")
            Log.w("AZURA_ASSIGN", "⚠️ ZERO ASSIGNMENTS FOUND DI CLOUD!")
        }
    }

    suspend fun assignToClass(faceId: String, classId: String) = withContext(Dispatchers.IO) {
        val assignment = FaceAssignmentEntity(
            faceId = faceId,
            classId = classId,
            schoolId = schoolId,
            isSynced = false
        )

        try {
            // ☁️ WAJIB: Push ke Cloud Dulu!
            syncFaceAssignmentToCloud(assignment)
            
            // 📱 Jika Cloud sukses, simpan di SQLite dengan status synced
            faceAssignmentDao.insertAssignment(assignment.copy(isSynced = true))
            Log.d("FaceAssignmentRepo", "✅ Berhasil assign siswa ke kelas $classId (Lokal & Cloud)")
        } catch (e: Exception) {
            Log.e("FaceAssignmentRepo", "❌ Gagal sync assignment ke Cloud: ${e.message}")
            // Lempar error agar UI tidak pura-pura sukses
            throw Exception("Gagal memasukkan siswa ke kelas di Server. Pastikan internet Anda aktif.")
        }
    }

    suspend fun removeSpecificAssignment(faceId: String, classId: String) = withContext(Dispatchers.IO) {
        try {
            // ☁️ WAJIB: Hapus di Cloud Dulu!
            val docId = "${faceId}_${classId}"
            db.collection("schools").document(schoolId)
              .collection("face_assignments").document(docId).delete().await()

            // 📱 Jika Cloud sukses, hapus di SQLite
            faceAssignmentDao.deleteSpecificAssignment(faceId, classId, schoolId)
            Log.d("FaceAssignmentRepo", "✅ Assignment dihapus secara lokal & Cloud.")
        } catch (e: Exception) {
            Log.e("FaceAssignmentRepo", "❌ Gagal menghapus assignment di Cloud: ${e.message}")
            throw Exception("Gagal menghapus relasi kelas di Server. Pastikan internet Anda aktif.")
        }
    }

    suspend fun removeAllAssignmentsForFace(faceId: String) = withContext(Dispatchers.IO) {
        try {
            // ☁️ WAJIB: Hapus di Cloud Dulu!
            val assignmentsSnapshot = db.collection("schools").document(schoolId)
              .collection("face_assignments").whereEqualTo("faceId", faceId).get().await()
              
            for (doc in assignmentsSnapshot.documents) {
                doc.reference.delete().await()
            }

            // 📱 Jika Cloud sukses, hapus di SQLite
            faceAssignmentDao.deleteAllByFace(faceId, schoolId)
            Log.d("FaceAssignmentRepo", "✅ Semua assignment untuk siswa dihapus secara lokal & Cloud.")
        } catch (e: Exception) {
            Log.e("FaceAssignmentRepo", "❌ Gagal menghapus semua assignment di Cloud: ${e.message}")
            throw Exception("Gagal menghapus relasi kelas di Server. Pastikan internet Anda aktif.")
        }
    }

    private suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity) {
        val docId = "${assignment.faceId}_${assignment.classId}"
        val data = hashMapOf("faceId" to assignment.faceId, "classId" to assignment.classId, "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        db.collection("schools").document(assignment.schoolId).collection("face_assignments").document(docId).set(data, SetOptions.merge()).await()
    }
}
