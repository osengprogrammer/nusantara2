package com.azuratech.azuratime.features.account.data.repo

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🏰 SCHOOL WORKSPACE REPOSITORY
 * Menangani perpindahan konteks antar sekolah (Switching Schools).
 * 🔥 v3.0: Full SSOT. Saves to Room first, sync happens in background.
 */
@Singleton
class SchoolWorkspaceRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val accessRequestRepository: AccessRequestRepository
) {
    private val accountDao     = database.accountDao()
    private val biometricDao   = database.biometricDao()
    private val recordDao      = database.attendanceRecordDao()
    private val classDao       = database.classDao()
    private val assignmentDao  = database.studentClassAssignmentDao()
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
    suspend fun createNewSchool(accountId: String, schoolName: String): String = withContext(Dispatchers.IO) {
        val schoolId = "sch_${System.currentTimeMillis()}"
        database.withTransaction {
            val school = SchoolEntity(
                id = schoolId,
                accountId = accountId,
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
    suspend fun switchWorkspace(accountId: String, newSchoolId: String) =
        withContext(Dispatchers.IO) {
            Log.w("AZURA_WORKSPACE", "🔄 Memulai perpindahan Workspace ke: $newSchoolId")

            val oldSchoolId = database.withTransaction {
                val account = accountDao.getAccountById(accountId)
                val oldId = account?.activeSchoolId ?: ""
                
                if (account != null) {
                    accountDao.updateAccount(account.copy(
                        activeSchoolId = newSchoolId,
                        syncStatus = SyncStatus.PENDING_UPDATE.name
                    ))
                }
                oldId
            }

            // 🧹 NUKE DATA TENANT LAMA (Optional, keep for v3.0 if desired)
            if (oldSchoolId.isNotEmpty() && oldSchoolId != newSchoolId) {
                Log.w("AZURA_WORKSPACE", "🧹 Menghapus data tenant lama ($oldSchoolId) dari SQLite...")
                biometricDao.deleteAllBySchool(oldSchoolId)
                recordDao.deleteAllBySchool(oldSchoolId)
                classDao.deleteBySchoolId(oldSchoolId)
                assignmentDao.deleteAllBySchool(oldSchoolId)
            }

            // 📥 TRIGGER BACKGROUND PULL/SYNC
            syncManager.enqueueProfileSync(accountId)
            syncManager.enqueueSync() // 🔥 RECOVERY: Pull classes and faces for the new school
            
            Log.w("AZURA_WORKSPACE", "✅ Workspace switched locally. Syncing in background...")
        }

    /**
     * 🔑 ASSIGN SCHOOL ROLE
     * Menetapkan role user di sebuah sekolah. Updates Room and triggers sync.
     */
    suspend fun assignSchoolRole(accountId: String, schoolId: String, role: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val account = accountDao.getAccountById(accountId)
                account?.let {
                    val updatedMemberships = it.memberships.toMutableMap().apply {
                        put(schoolId, Membership(schoolName = schoolName, role = role))
                    }
                    accountDao.updateAccount(it.copy(
                        memberships = updatedMemberships,
                        syncStatus = SyncStatus.PENDING_UPDATE.name
                    ))
                    
                    // Trigger both profile (for memberships) and access sync as requested
                    syncManager.enqueueProfileSync(accountId)
                    syncManager.enqueueAccessSync(accountId)
                    
                    Log.i("AZURA_WORKSPACE", "🔑 Assigned $role role locally for school $schoolId")
                }
            }
        }

    /**
     * 🚪 REQUEST TO JOIN
     * Mengajukan diri untuk bergabung ke sebuah sekolah/instansi via Repository.
     */
    suspend fun requestToJoinWorkspace(accountId: String, schoolId: String, schoolName: String) =
        withContext(Dispatchers.IO) {
            accessRequestRepository.submitRequest(accountId, schoolId, schoolName)
            Log.i("AZURA_WORKSPACE", "✅ Request join submitted via Repository for: $schoolName")
        }
}
