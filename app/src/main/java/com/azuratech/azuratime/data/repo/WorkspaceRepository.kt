package com.azuratech.azuratime.data.repo

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.domain.user.usecase.SubmitSchoolAccessUseCase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 WORKSPACE REPOSITORY
 * Menangani perpindahan konteks antar sekolah (Switching Schools).
 * 🔥 v3.0: Full SSOT. Saves to Room first, sync happens in background.
 */
@Singleton
class WorkspaceRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val submitSchoolAccessUseCase: SubmitSchoolAccessUseCase
) {
    private val userDao        = database.userDao()
    private val faceDao        = database.faceDao()
    private val recordDao      = database.checkInRecordDao()
    private val classDao       = database.classDao()
    private val assignmentDao  = database.faceAssignmentDao()
    private val schoolDao      = database.schoolDao()

    /**
     * Search schools by name from Local Room DB (SSOT).
     */
    suspend fun searchSchools(query: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val schools = schoolDao.searchSchools(query)
            schools.map { school ->
                mapOf(
                    "schoolId" to school.id,
                    "schoolName" to school.name,
                    "status" to school.status
                )
            }
        } catch (e: Exception) {
            Log.e("AZURA_WORKSPACE", "❌ Error searching schools locally: ${e.message}")
            emptyList()
        }
    }

    /**
     * 🔥 Create School: SSOT way.
     * Return ID immediately after saving to Room.
     */
    suspend fun createNewSchool(userId: String, userEmail: String, schoolName: String): String = withContext(Dispatchers.IO) {
        val schoolId = "sch_${System.currentTimeMillis()}"
        database.withTransaction {
            val school = SchoolEntity(
                id = schoolId,
                accountId = userId,
                name = schoolName,
                timezone = "Asia/Jakarta",
                status = "PENDING",
                syncStatus = SyncStatus.PENDING_INSERT.name
            )
            schoolDao.insertSchool(school)
            syncManager.enqueueSchoolSync(schoolId)
        }
        schoolId
    }

    /**
     * Finalize setup by activating school status.
     */
    suspend fun finalizeSetup(schoolId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val school = schoolDao.getSchoolById(schoolId)
            if (school != null) {
                schoolDao.insertSchool(school.copy(
                    status = "ACTIVE",
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                ))
                syncManager.enqueueSchoolSync(schoolId)
            }
        }
    }

    /**
     * Update school name locally and enqueue sync.
     */
    suspend fun updateSchoolName(schoolId: String, newName: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val school = schoolDao.getSchoolById(schoolId)
            if (school != null) {
                schoolDao.insertSchool(school.copy(
                    name = newName.trim(),
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                ))
                syncManager.enqueueSchoolSync(schoolId)
            }
        }
    }

    /**
     * 🔥 THE WORKSPACE SWITCH ENGINE (v3.0 SSOT)
     * Mengganti "dunia" aktif user dan membersihkan data tenant lama.
     * Pull data remote dilakukan oleh ProfileSyncWorker di background.
     */
    suspend fun switchWorkspace(userId: String, newSchoolId: String) =
        withContext(Dispatchers.IO) {
            Log.w("AZURA_WORKSPACE", "🔄 Memulai perpindahan Workspace ke: $newSchoolId")

            val oldSchoolId = database.withTransaction {
                val user = userDao.getUserById(userId)
                val oldId = user?.activeSchoolId ?: ""
                
                if (user != null) {
                    userDao.updateUser(user.copy(
                        activeSchoolId = newSchoolId,
                        syncStatus = SyncStatus.PENDING_UPDATE.name
                    ))
                }
                oldId
            }

            // 🧹 NUKE DATA TENANT LAMA (Optional, keep for v3.0 if desired)
            if (oldSchoolId.isNotEmpty() && oldSchoolId != newSchoolId) {
                Log.w("AZURA_WORKSPACE", "🧹 Menghapus data tenant lama ($oldSchoolId) dari SQLite...")
                faceDao.deleteAllBySchool(oldSchoolId)
                recordDao.deleteAllBySchool(oldSchoolId)
                classDao.deleteBySchoolId(oldSchoolId)
                assignmentDao.deleteAllBySchool(oldSchoolId)
            }

            // 📥 TRIGGER BACKGROUND PULL/SYNC
            syncManager.enqueueProfileSync(userId)
            
            Log.w("AZURA_WORKSPACE", "✅ Workspace switched locally. Syncing in background...")
        }

    /**
     * 🔑 ASSIGN SCHOOL ROLE
     * Menetapkan role user di sebuah sekolah. Updates Room and triggers sync.
     */
    suspend fun assignSchoolRole(userId: String, schoolId: String, role: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val user = userDao.getUserById(userId)
                user?.let {
                    val updatedMemberships = it.memberships.toMutableMap().apply {
                        put(schoolId, Membership(schoolName = schoolName, role = role))
                    }
                    userDao.updateUser(it.copy(
                        memberships = updatedMemberships,
                        syncStatus = SyncStatus.PENDING_UPDATE.name
                    ))
                    
                    // Trigger both profile (for memberships) and access sync as requested
                    syncManager.enqueueProfileSync(userId)
                    syncManager.enqueueAccessSync(userId)
                    
                    Log.i("AZURA_WORKSPACE", "🔑 Assigned $role role locally for school $schoolId")
                }
            }
        }

    /**
     * 🚪 REQUEST TO JOIN
     * Mengajukan diri untuk bergabung ke sebuah sekolah/instansi via UseCase.
     */
    suspend fun requestToJoinWorkspace(userId: String, schoolId: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            submitSchoolAccessUseCase(userId, schoolId, schoolName, "TEACHER")
            Log.i("AZURA_WORKSPACE", "✅ Request join submitted via UseCase for: $schoolName")
        }
}
