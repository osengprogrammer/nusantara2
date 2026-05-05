package com.azuratech.azuratime.data.repo

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 WORKSPACE REPOSITORY
 * Menangani perpindahan konteks antar sekolah (Switching Schools).
 * 🔥 Sudah menggunakan Hilt Dependency Injection.
 */
@Singleton
class WorkspaceRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val userDao        = database.userDao()
    private val faceDao        = database.faceDao()
    private val recordDao      = database.checkInRecordDao()
    private val classDao       = database.classDao()
    private val assignmentDao  = database.faceAssignmentDao()

    private fun getTenantRef(schoolId: String) = db.collection("schools").document(schoolId)

    /**
     * Search schools by name from Firestore.
     */
    suspend fun searchSchools(query: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("schools")
                .whereGreaterThanOrEqualTo("schoolName", query)
                .whereLessThanOrEqualTo("schoolName", query + "\uf8ff")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 🔥 THE WORKSPACE SWITCH ENGINE
     * Mengganti "dunia" aktif user dan membersihkan data tenant lama.
     */
    suspend fun switchWorkspace(userId: String, newSchoolId: String) =
        withContext(Dispatchers.IO) {
            Log.w("AZURA_WORKSPACE", "🔄 Memulai perpindahan Workspace ke: $newSchoolId")

            // 1. Update Cloud (Firestore)
            db.collection("whitelisted_users").document(userId).update("activeSchoolId", newSchoolId).await()

            // 2. Update Local User Context
            val currentUser = userDao.getUserById(userId)
            val oldSchoolId = currentUser?.activeSchoolId ?: ""

            if (currentUser != null) {
                userDao.updateUser(currentUser.copy(activeSchoolId = newSchoolId))
            }

            // 3. 🧹 NUKE DATA TENANT LAMA (Pembersihan Lahan)
            if (oldSchoolId.isNotEmpty() && oldSchoolId != newSchoolId) {
                Log.w("AZURA_WORKSPACE", "🧹 Menghapus data tenant lama ($oldSchoolId) dari SQLite...")
                faceDao.deleteAllBySchool(oldSchoolId)
                recordDao.deleteAllBySchool(oldSchoolId)
                classDao.deleteBySchoolId(oldSchoolId)
                assignmentDao.deleteAllBySchool(oldSchoolId)
            }

            // 4. 📥 DOWNLOAD DATA BARU (Sinkronisasi Langit ke Bumi)
            Log.w("AZURA_WORKSPACE", "📥 Mengunduh Master Data untuk Workspace: $newSchoolId...")

            // Download Classes
            val classSnapshot = getTenantRef(newSchoolId).collection("classes").get().await()
            val newClasses = classSnapshot.documents.map { doc ->
                ClassEntity(
                    id = doc.id,
                    accountId = userId,
                    schoolId = newSchoolId,
                    name = doc.getString("name") ?: "",
                    displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0,
                    isSynced = true
                )
            }
            newClasses.forEach { 
                classDao.insert(it) 
                // 🔥 Also create assignment in the new join table
                database.schoolClassDao().assignClass(
                    com.azuratech.azuratime.data.local.SchoolClassAssignment(newSchoolId, it.id)
                )
            }

            // Download Faces & Biometric Embedding (Full Sync)
            val faceSnapshot = getTenantRef(newSchoolId).collection("master_faces").get().await()
            val newFaces = faceSnapshot.documents.mapNotNull { doc ->
                val isActive = doc.getBoolean("isActive") ?: true
                if (!isActive) return@mapNotNull null
                
                val embedding = (doc.get("embedding") as? List<*>)?.map { (it as Number).toFloat() }?.toFloatArray()
                FaceEntity(
                    faceId = doc.id,
                    schoolId = newSchoolId,
                    name = doc.getString("name") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    embedding = embedding,
                    isSynced = true
                )
            }
            faceDao.upsertAll(newFaces)

            // Download Assignments (Relasi Siswa-Kelas)
            val assignmentSnapshot = getTenantRef(newSchoolId).collection("face_assignments").get().await()
            val newAssignments = assignmentSnapshot.documents.mapNotNull { doc ->
                val faceId = doc.getString("faceId") ?: return@mapNotNull null
                val classId = doc.getString("classId") ?: return@mapNotNull null
                FaceAssignmentEntity(faceId = faceId, classId = classId, schoolId = newSchoolId, isSynced = true)
            }
            newAssignments.forEach { assignmentDao.insertAssignment(it) }

            // 🔥 REFRESH FACE CACHE (Memory Scanner)
            FaceCache.refresh(application, newSchoolId)

            Log.w("AZURA_WORKSPACE", "✅ Workspace berhasil dipindah!")
        }

    /**
     * 🔑 ASSIGN SCHOOL ROLE
     * Menetapkan role user di sebuah sekolah baik lokal maupun cloud (Firestore & SQLite).
     */
    suspend fun assignSchoolRole(userId: String, schoolId: String, role: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            val newMembership = Membership(
                schoolName = schoolName,
                role = role
            )

            // 1. Update Cloud (Firestore)
            try {
                db.collection("whitelisted_users").document(userId)
                    .update(
                        mapOf(
                            "memberships.$schoolId.schoolName" to schoolName,
                            "memberships.$schoolId.role" to role
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e("AZURA_WORKSPACE", "⚠️ Gagal update role di Firestore: ${e.message}")
            }

            // 2. Update Local (Room)
            val user = userDao.getUserById(userId)
            user?.let {
                val updatedMemberships = it.memberships.toMutableMap().apply {
                    put(schoolId, newMembership)
                }
                userDao.updateUser(it.copy(memberships = updatedMemberships))
                println("🔑 DEBUG: Assigned $role role for school $schoolId to user $userId")
            }
        }

    /**
     * 🚪 REQUEST TO JOIN
     * Mengajukan diri untuk bergabung ke sebuah sekolah/instansi.
     */
    suspend fun requestToJoinWorkspace(userId: String, schoolId: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            val newMembership = com.azuratech.azuratime.data.local.Membership(
                schoolName = schoolName,
                role = "PENDING"
            )

            // 1. Kirim ke Firestore
            db.collection("whitelisted_users").document(userId)
                .update(
                    mapOf(
                        "memberships.$schoolId.schoolName" to schoolName,
                        "memberships.$schoolId.role" to "PENDING"
                    )
                ).await()

            // 2. Update UI lokal agar status berubah jadi "Waiting Approval"
            val user = userDao.getUserById(userId)
            user?.let {
                val updatedMemberships = it.memberships.toMutableMap().apply {
                    put(schoolId, newMembership)
                }
                userDao.insertUser(it.copy(memberships = updatedMemberships))
            }

            Log.i("AZURA_WORKSPACE", "✅ Request join dikirim: $schoolName")
        }
}
