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
    database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val faceAssignmentDao = database.faceAssignmentDao()
    private val classDao = database.classDao()

    // 🔥 ID Sekolah dinamis dari SessionManager
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    val allAssignedClassesMap: Flow<Map<String, List<ClassEntity>>> =
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

    // =====================================================
    // 📖 READ OPERATIONS
    // =====================================================

    fun getAssignedClassIds(faceId: String): Flow<List<String>> {
        return faceAssignmentDao.getClassIdsForFace(faceId, schoolId)
    }

    fun getAssignedClassesDetails(faceId: String): Flow<List<ClassEntity>> {
        return faceAssignmentDao.getClassIdsForFace(faceId, schoolId).map { classIds ->
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

        try {
            val collectionRef = db.collection("schools").document(schoolId).collection("face_assignments")
            val snapshot = collectionRef.get().await()

            if (snapshot.documents.isNotEmpty()) {
                val assignments = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    val faceId = data?.get("faceId") as? String
                    val classId = data?.get("classId") as? String
                    if (faceId != null && classId != null) {
                        FaceAssignmentEntity(
                            faceId = faceId,
                            classId = classId,
                            schoolId = schoolId,
                            isSynced = true
                        )
                    } else null
                }
                
                // Optional: For robust sync, we might want to clear local assignments for this school 
                // and replace them with server ones, but Room Foreign Keys might be tricky if the 
                // face/class isn't synced yet. Since SyncWorker syncs Classes -> Faces -> Assignments, 
                // it should be safe.
                
                for (assignment in assignments) {
                    try {
                        faceAssignmentDao.insertAssignment(assignment)
                    } catch (e: Exception) {
                        Log.e("FaceAssignmentRepo", "Foreign key issue skipping assignment: ${assignment.faceId}")
                    }
                }
                Log.i("FaceAssignmentRepo", "✅ Pulled ${assignments.size} face assignments from cloud")
            }
        } catch (e: Exception) {
            Log.e("FaceAssignmentRepo", "❌ Assignment Sync Failed: ${e.message}")
        }
    }

    suspend fun assignToClass(faceId: String, classId: String) = withContext(Dispatchers.IO) {
        val assignment = FaceAssignmentEntity(
            faceId = faceId,
            classId = classId,
            schoolId = schoolId,
            isSynced = false
        )

        faceAssignmentDao.insertAssignment(assignment)

        try {
            syncFaceAssignmentToCloud(assignment)
            faceAssignmentDao.insertAssignment(assignment.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FaceAssignmentRepo", "Gagal sync assignment: ${e.message}")
        }
    }

    suspend fun removeSpecificAssignment(faceId: String, classId: String) = withContext(Dispatchers.IO) {
        faceAssignmentDao.deleteSpecificAssignment(faceId, classId, schoolId)
        Log.d("FaceAssignmentRepo", "Assignment dihapus secara lokal.")
    }

    suspend fun removeAllAssignmentsForFace(faceId: String) = withContext(Dispatchers.IO) {
        faceAssignmentDao.deleteAllByFace(faceId, schoolId)
        Log.d("FaceAssignmentRepo", "Semua assignment dihapus secara lokal.")
    }

    private suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity) {
        val docId = "${assignment.faceId}_${assignment.classId}"
        val data = hashMapOf("faceId" to assignment.faceId, "classId" to assignment.classId, "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        db.collection("schools").document(assignment.schoolId).collection("face_assignments").document(docId).set(data, SetOptions.merge()).await()
    }
}
