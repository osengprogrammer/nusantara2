package com.azuratech.azuratime.features.school.data.repo

import androidx.room.withTransaction
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class SchoolRepository @Inject constructor(
    private val database: AppDatabase,
    private val remoteDataSource: SchoolRemoteDataSource,
    private val syncManager: SyncManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) {
    private val dao = database.schoolClassDao()
    private val schoolDao = database.schoolDao()
    private val userDao = database.accountDao()
    private val accessRequestDao = database.accessRequestDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observeSchools(accountId: String): Flow<Result<List<School>>> =
        dao.getSchools(accountId)
            .map { entities -> 
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e -> 
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    fun observeSchoolsByIds(schoolIds: List<String>): Flow<Result<List<School>>> =
        dao.observeSchoolsByIds(schoolIds)
            .map { entities -> 
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e -> 
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    fun observeAllSchools(): Flow<Result<List<School>>> =
        dao.observeAllSchools()
            .map { entities -> 
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e -> 
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    suspend fun createSchool(adminId: String, name: String, timezone: String): Result<String> {
        return try {
            val schoolId = "sch_${System.currentTimeMillis()}"
            database.withTransaction {
                val school = SchoolEntity(
                    id = schoolId,
                    accountId = adminId,
                    name = name,
                    timezone = timezone,
                    status = "ACTIVE",
                    syncStatus = SyncStatus.PENDING_INSERT.name
                )
                dao.upsertSchool(school)

                val requestId = "req_creator_$schoolId"
                accessRequestDao.insertRequest(AccessRequestEntity(
                    requestId = requestId,
                    requesterId = adminId,
                    schoolId = schoolId,
                    schoolName = name,
                    status = AccessRequestStatus.APPROVED,
                    syncStatus = SyncStatus.PENDING_INSERT
                ))

                val account = database.accountDao().getAccountById(adminId)
                if (account != null) {
                    val updatedMemberships = account.memberships.toMutableMap()
                    updatedMemberships[schoolId] = Membership(
                        schoolName = name,
                        role = "ADMIN"
                    )
                    database.accountDao().updateAccount(account.copy(
                        memberships = updatedMemberships,
                        activeSchoolId = schoolId,
                        syncStatus = SyncStatus.PENDING_UPDATE.name
                    ))
                }

                syncManager.enqueueSchoolSync(schoolId)
                syncManager.enqueueProfileSync(adminId)
            }
            Result.Success(schoolId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun updateSchoolDetails(schoolId: String, name: String?, timezone: String?): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = dao.getSchoolById(schoolId) ?: return@withTransaction
                val updated = existing.copy(
                    name = name ?: existing.name,
                    timezone = timezone ?: existing.timezone,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPDATE.name
                )
                dao.upsertSchool(updated)
                syncManager.enqueueSchoolSync(schoolId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun saveSchool(school: School): Result<Unit> = try {
        saveSchoolLocally(school)
        syncManager.enqueueSchoolSync(school.id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun saveSchoolLocally(school: School) {
        dao.upsertSchool(
            SchoolEntity(
                id = school.id,
                accountId = school.accountId,
                name = school.name,
                timezone = school.timezone,
                status = school.status,
                createdAt = school.createdAt,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED.name
            )
        )
    }

    suspend fun getSchoolById(id: String): School? = 
        dao.getSchoolById(id)?.toDomain()

    suspend fun getCountByUser(accountId: String): Int = dao.getSchoolCountByAccount(accountId)

    suspend fun getFirstSchoolId(accountId: String): String? = dao.getFirstSchoolId(accountId)

    suspend fun schoolExists(schoolId: String): Boolean = dao.getSchoolById(schoolId) != null

    suspend fun syncClasses(accountId: String, schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            println("🔄 SYNC: Starting syncClasses for school: $schoolId (Account: $accountId)")
            val remoteResult = remoteDataSource.getClasses(accountId, schoolId)
            if (remoteResult is Result.Success) {
                println("🔄 SYNC: Remote fetch success. Processing ${remoteResult.data.size} classes.")
                remoteResult.data.forEach { classModel ->
                    println("🔄 SYNC: Saving class locally: ${classModel.name} (${classModel.id})")
                    // 🔥 SSOT RECOVERY: Ensure class exists locally
                    saveClassLocally(
                        ClassEntity(
                            id = classModel.id,
                            accountId = accountId,
                            schoolId = classModel.schoolId ?: schoolId, // Ensure it's linked to the owner school
                            name = classModel.name,
                            grade = classModel.grade,
                            teacherId = classModel.teacherId,
                            studentCount = classModel.studentCount,
                            createdAt = classModel.createdAt
                        )
                    )
                    
                    // 🔥 JOIN TABLE RECOVERY: Ensure the assignment exists in Room after reinstall
                    dao.assignClass(SchoolClassAssignment(schoolId, classModel.id))
                }
                println("✅ SYNC: All ${remoteResult.data.size} classes saved and assigned.")
            } else if (remoteResult is Result.Failure) {
                println("❌ SYNC: Remote fetch failed: ${remoteResult.error.message}")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ SYNC: Unexpected error in syncClasses: ${e.message}")
            Result.Failure(AppError.Network(e.message))
        }
    }

    suspend fun deleteSchool(id: String, @Suppress("UNUSED_PARAMETER") accountId: String): Result<Unit> = try {
        database.withTransaction {
            val existing = dao.getSchoolById(id)
            if (existing != null) {
                dao.upsertSchool(existing.copy(
                    status = "DELETED",
                    syncStatus = SyncStatus.PENDING_DELETE.name
                ))
                syncManager.enqueueSchoolSync(id)
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    // =====================================================
    // 🏫 CLASS OPERATIONS
    // =====================================================

    fun observeClasses(schoolId: String): Flow<Result<List<ClassModel>>> =
        dao.getClasses(schoolId)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<ClassModel>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    suspend fun saveClass(_accountId: String, schoolId: String?, classModel: ClassModel): Result<Unit> {
        return try {
            val entity = ClassEntity(
                id = classModel.id,
                accountId = _accountId,
                schoolId = schoolId, 
                name = classModel.name,
                grade = classModel.grade,
                teacherId = classModel.teacherId,
                studentCount = classModel.studentCount,
                createdAt = classModel.createdAt
            )
            dao.upsertClass(entity)

            if (schoolId != null) {
                dao.assignClass(SchoolClassAssignment(schoolId, classModel.id))
            }

            repositoryScope.launch {
                val remoteSchoolId = schoolId ?: "global"
                try {
                    remoteDataSource.saveClass(_accountId, remoteSchoolId, classModel)
                } catch (e: Exception) {
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun assignClassToSchool(schoolId: String, classId: String): Result<Unit> = try {
        dao.assignClass(SchoolClassAssignment(schoolId, classId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun unassignClassFromSchool(schoolId: String, classId: String): Result<Unit> = try {
        dao.unassignClass(schoolId, classId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun getAssignedClassIds(schoolId: String): List<String> = dao.getAssignedClassIds(schoolId)

    suspend fun deleteClass(_accountId: String, schoolId: String, classId: String): Result<Unit> {
        return try {
            val studentCount = dao.getStudentCountForClass(schoolId, classId)
            if (studentCount > 0) {
                return Result.Failure(AppError.BusinessRule("Gagal! Masih ada $studentCount siswa di kelas ini."))
            }

            dao.deleteClassById(classId)

            repositoryScope.launch {
                remoteDataSource.deleteClass(_accountId, schoolId, classId)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun saveClassLocally(classEntity: ClassEntity) {
        dao.upsertClass(classEntity)
    }

    fun getLocalClasses(schoolId: String): Flow<List<ClassEntity>> {
        return dao.getClasses(schoolId)
    }

    fun observeAllClassesForAccount(accountId: String): Flow<Result<List<ClassModel>>> =
        dao.getAllClasses(accountId).map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<ClassModel>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    suspend fun reassignClass(@Suppress("UNUSED_PARAMETER") accountId: String, classId: String, newSchoolId: String): Result<Unit> = try {
        dao.reassignClass(SchoolClassAssignment(newSchoolId, classId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun getOrphanedClasses(): List<ClassModel> =
        dao.getOrphanedClasses().map { it.toDomain() }

    suspend fun updateClassSchool(classId: String, schoolId: String) {
        dao.updateClassSchool(classId, schoolId)
        dao.assignClass(SchoolClassAssignment(schoolId, classId))
    }

    suspend fun approveSchool(schoolId: String): Result<Unit> = try {
        database.withTransaction {
            val school = dao.getSchoolById(schoolId)
            if (school != null) {
                dao.upsertSchool(school.copy(status = "ACTIVE", syncStatus = SyncStatus.PENDING_UPDATE.name))
                syncManager.enqueueSchoolSync(schoolId)
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun rejectSchool(schoolId: String, @Suppress("UNUSED_PARAMETER") reason: String): Result<Unit> = try {
        database.withTransaction {
            val school = dao.getSchoolById(schoolId)
            if (school != null) {
                dao.upsertSchool(school.copy(status = "REJECTED", syncStatus = SyncStatus.PENDING_UPDATE.name))
                syncManager.enqueueSchoolSync(schoolId)
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun pushSchool(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val school = dao.getSchoolById(schoolId) ?: return@withContext Result.Failure(AppError.LocalDB("School not found: $schoolId"))

            if (school.syncStatus == SyncStatus.SYNCED.name) {
                return@withContext Result.Success(Unit)
            }

            val schoolData = mutableMapOf<String, Any>(
                "schoolId" to school.id,
                "accountId" to school.accountId,
                "schoolName" to school.name,
                "timezone" to school.timezone,
                "status" to school.status,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection("schools").document(schoolId)

            when (school.syncStatus) {
                SyncStatus.PENDING_INSERT.name -> {
                    schoolData["createdAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                    com.google.android.gms.tasks.Tasks.await(docRef.set(schoolData))
                }
                SyncStatus.PENDING_UPDATE.name, SyncStatus.PENDING_DELETE.name -> {
                    com.google.android.gms.tasks.Tasks.await(docRef.update(schoolData))
                }
            }

            dao.upsertSchool(school.copy(syncStatus = SyncStatus.SYNCED.name))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    suspend fun pushAccessRequests(@Suppress("UNUSED_PARAMETER") userId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val unsynced = accessRequestDao.getUnsyncedRequestsByUser(userId)
            if (unsynced.isEmpty()) return@withContext Result.Success(Unit)

            for (request in unsynced) {
                val requestData = mapOf(
                    "requestId" to request.requestId,
                    "requesterId" to request.requesterId,
                    "schoolId" to request.schoolId,
                    "schoolName" to request.schoolName,
                    "status" to request.status.name,
                    "createdAt" to request.createdAt,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("access_requests")
                        .document(request.requestId)
                        .set(requestData)
                )

                accessRequestDao.insertRequest(request.copy(
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis()
                ))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
