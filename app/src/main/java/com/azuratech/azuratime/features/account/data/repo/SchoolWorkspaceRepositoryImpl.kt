package com.azuratech.azuratime.features.account.data.repo

import android.app.Application
import androidx.room.withTransaction
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolWorkspaceRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val accessRequestRepository: AccessRequestRepository,
) : SchoolWorkspaceRepository {
    private val accountDao = database.accountDao()
    private val biometricDao = database.biometricDao()
    private val recordDao = database.attendanceRecordDao()
    private val classDao = database.classDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val schoolDao = database.schoolDao()

    override suspend fun searchSchools(query: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val schools = schoolDao.searchSchools(query)
            Result.Success(
                schools.map { school ->
                    mapOf(
                        "schoolId" to school.id,
                        "schoolName" to school.name,
                        "status" to school.status,
                    )
                },
            )
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun createNewSchool(accountId: String, schoolName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val schoolId = "sch_${System.currentTimeMillis()}"
            database.withTransaction {
                val school = SchoolEntity(
                    id = schoolId,
                    accountId = accountId,
                    name = schoolName,
                    timezone = "Asia/Jakarta",
                    status = "PENDING",
                    syncStatus = SyncStatus.PENDING_INSERT.name,
                )
                schoolDao.insertSchool(school)
                syncManager.enqueueSchoolSync(schoolId)
            }
            Result.Success(schoolId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun finalizeSetup(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val school = schoolDao.getSchoolById(schoolId)
                if (school != null) {
                    schoolDao.insertSchool(
                        school.copy(
                            status = "ACTIVE",
                            syncStatus = SyncStatus.PENDING_UPDATE.name,
                        ),
                    )
                    syncManager.enqueueSchoolSync(schoolId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateSchoolName(schoolId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val school = schoolDao.getSchoolById(schoolId)
                if (school != null) {
                    schoolDao.insertSchool(
                        school.copy(
                            name = newName.trim(),
                            syncStatus = SyncStatus.PENDING_UPDATE.name,
                        ),
                    )
                    syncManager.enqueueSchoolSync(schoolId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun switchWorkspace(accountId: String, newSchoolId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val oldSchoolId = database.withTransaction {
                    val account = accountDao.getAccountById(accountId)
                    val oldId = account?.activeSchoolId ?: ""

                    if (account != null) {
                        accountDao.updateAccount(
                            account.copy(
                                activeSchoolId = newSchoolId,
                                syncStatus = SyncStatus.PENDING_UPDATE.name,
                            ),
                        )
                    }
                    oldId
                }

                if (oldSchoolId.isNotEmpty() && oldSchoolId != newSchoolId) {
                    biometricDao.deleteAllBySchool(oldSchoolId)
                    recordDao.deleteAllBySchool(oldSchoolId)
                    classDao.deleteBySchoolId(oldSchoolId)
                    assignmentDao.deleteAllBySchool(oldSchoolId)
                }

                syncManager.enqueueProfileSync(accountId)
                syncManager.enqueueSync()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(AppError.LocalDB(e.message))
            }
        }

    override suspend fun assignSchoolRole(accountId: String, schoolId: String, role: String, schoolName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    val account = accountDao.getAccountById(accountId)
                    account?.let {
                        val updatedMemberships = it.memberships.toMutableMap().apply {
                            put(schoolId, Membership(schoolName = schoolName, role = role))
                        }
                        accountDao.updateAccount(
                            it.copy(
                                memberships = updatedMemberships,
                                syncStatus = SyncStatus.PENDING_UPDATE.name,
                            ),
                        )

                        syncManager.enqueueProfileSync(accountId)
                        syncManager.enqueueAccessSync(accountId)
                    }
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(AppError.LocalDB(e.message))
            }
        }

    override suspend fun requestToJoinWorkspace(accountId: String, schoolId: String, schoolName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            accessRequestRepository.submitRequest(accountId, schoolId, schoolName)
        }
}
