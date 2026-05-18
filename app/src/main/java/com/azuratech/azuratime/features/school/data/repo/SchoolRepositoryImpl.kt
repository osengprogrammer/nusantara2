package com.azuratech.azuratime.features.school.data.repo

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val remoteDataSource: SchoolRemoteDataSource,
    private val syncManager: SyncManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
) : SchoolRepository {
    private val dao = database.schoolClassDao()
    private val schoolDao = database.schoolDao()
    private val userDao = database.accountDao()
    private val accessRequestDao = database.accessRequestDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun observeSchools(accountId: String): Flow<Result<List<School>>> =
        dao.getSchools(accountId)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    override fun observeSchoolsByIds(schoolIds: List<String>): Flow<Result<List<School>>> =
        dao.observeSchoolsByIds(schoolIds)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    override fun observeSchoolById(id: String): Flow<Result<School?>> =
        dao.observeSchoolById(id)
            .map { Result.Success(it?.toDomain()) as Result<School?> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override fun observeAllSchools(): Flow<Result<List<School>>> =
        dao.observeAllSchools()
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    override suspend fun createSchool(adminId: String, name: String, timezone: String): Result<String> {
        return try {
            val schoolId = "sch_${System.currentTimeMillis()}"
            database.withTransaction {
                val school = SchoolEntity(
                    id = schoolId,
                    accountId = adminId,
                    name = name,
                    timezone = timezone,
                    status = "ACTIVE",
                    syncStatus = SyncStatus.PENDING_INSERT.name,
                )
                dao.upsertSchool(school)

                val requestId = "req_creator_$schoolId"
                accessRequestDao.insertRequest(
                    AccessRequestEntity(
                        requestId = requestId,
                        accountId = adminId,
                        schoolId = schoolId,
                        schoolName = name,
                        status = AccessRequestStatus.APPROVED,
                        syncStatus = SyncStatus.PENDING_INSERT,
                    ),
                )

                val user = database.accountDao().getAccountById(adminId)
                if (user != null) {
                    val updatedMemberships = user.memberships.toMutableMap()
                    updatedMemberships[schoolId] = Membership(
                        schoolName = name,
                        role = "ADMIN",
                    )
                    database.accountDao().updateAccount(
                        user.copy(
                            memberships = updatedMemberships,
                            activeSchoolId = schoolId,
                            syncStatus = SyncStatus.PENDING_UPDATE.name,
                        ),
                    )
                }

                syncManager.enqueueSchoolSync(schoolId)
                syncManager.enqueueProfileSync(adminId)
            }
            Result.Success(schoolId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateSchoolDetails(schoolId: String, name: String?, timezone: String?): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = dao.getSchoolById(schoolId) ?: return@withTransaction
                val updated = existing.copy(
                    name = name ?: existing.name,
                    timezone = timezone ?: existing.timezone,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPDATE.name,
                )
                dao.upsertSchool(updated)
                syncManager.enqueueSchoolSync(schoolId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveSchool(school: School): Result<Unit> = try {
        saveSchoolLocally(school)
        syncManager.enqueueSchoolSync(school.id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun saveSchoolLocally(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.upsertSchool(
                SchoolEntity(
                    id = school.id,
                    accountId = school.accountId,
                    name = school.name,
                    timezone = school.timezone,
                    status = school.status,
                    createdAt = school.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.SYNCED.name,
                ),
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getSchoolById(id: String): Result<School> = try {
        val entity = dao.getSchoolById(id)
        if (entity != null) {
            Result.Success(entity.toDomain())
        } else {
            Result.Failure(AppError.LocalDB("School not found"))
        }
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun getCountByAccount(accountId: String): Result<Int> = try {
        Result.Success(dao.getSchoolCountByAccount(accountId))
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun getFirstSchoolId(accountId: String): Result<String?> = try {
        Result.Success(dao.getFirstSchoolId(accountId))
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun schoolExists(schoolId: String): Result<Boolean> = try {
        Result.Success(dao.getSchoolById(schoolId) != null)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun syncSchools(schoolIds: List<String>): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            if (schoolIds.isEmpty()) return@withContext Result.Success(Unit)

            val remoteResult = remoteDataSource.getSchoolsByIds(schoolIds)
            if (remoteResult is Result.Success) {
                remoteResult.data.forEach { school ->
                    saveSchoolLocally(school)
                }
                Result.Success(Unit)
            } else {
                remoteResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncClasses(accountId: String, schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val remoteResult = remoteDataSource.getClasses(accountId, schoolId)
            if (remoteResult is Result.Success) {
                remoteResult.data.forEach { classModel ->
                    saveClassLocally(
                        ClassEntity(
                            id = classModel.id,
                            ownerAccountId = accountId,
                            schoolId = classModel.schoolId ?: schoolId,
                            name = classModel.name,
                            grade = classModel.grade,
                            accountId = classModel.accountId,
                            studentCount = classModel.studentCount,
                            createdAt = classModel.createdAt,
                        ),
                    )
                    dao.assignClass(SchoolClassAssignment(schoolId, classModel.id))
                }
                Result.Success(Unit)
            } else {
                remoteResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteSchool(id: String, accountId: String): Result<Unit> = try {
        database.withTransaction {
            val existing = dao.getSchoolById(id)
            if (existing != null) {
                dao.upsertSchool(
                    existing.copy(
                        status = "DELETED",
                        syncStatus = SyncStatus.PENDING_DELETE.name,
                    ),
                )
                syncManager.enqueueSchoolSync(id)
            }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override fun observeClasses(schoolId: String): Flow<Result<List<ClassModel>>> =
        dao.getClasses(schoolId)
            .map { entities ->
                Result.Success(entities.map { it.toDomain() }) as Result<List<ClassModel>>
            }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    override suspend fun getClasses(schoolId: String): Result<List<ClassModel>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getClasses(schoolId).first()
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveClass(_accountId: String, schoolId: String?, classModel: ClassModel): Result<Unit> {
        return try {
            val entity = ClassEntity(
                id = classModel.id,
                ownerAccountId = _accountId,
                schoolId = schoolId,
                name = classModel.name,
                grade = classModel.grade,
                accountId = classModel.accountId,
                studentCount = classModel.studentCount,
                createdAt = classModel.createdAt,
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

    override suspend fun assignClassToSchool(schoolId: String, classId: String): Result<Unit> = try {
        dao.assignClass(SchoolClassAssignment(schoolId, classId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun unassignClassFromSchool(schoolId: String, classId: String): Result<Unit> = try {
        dao.unassignClass(schoolId, classId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun getAssignedClassIds(schoolId: String): Result<List<String>> = try {
        Result.Success(dao.getAssignedClassIds(schoolId))
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun deleteClass(_accountId: String, schoolId: String, classId: String): Result<Unit> {
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

    override suspend fun saveClassLocally(classEntity: ClassEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.upsertClass(classEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun getLocalClasses(schoolId: String): Flow<Result<List<ClassEntity>>> =
        dao.getClasses(schoolId)
            .map { Result.Success(it) as Result<List<ClassEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override fun observeAllClassesForAccount(accountId: String): Flow<Result<List<ClassModel>>> =
        dao.getAllClasses(accountId).map { entities ->
            Result.Success(entities.map { it.toDomain() }) as Result<List<ClassModel>>
        }
            .catch { e ->
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    override suspend fun reassignClass(accountId: String, classId: String, newSchoolId: String): Result<Unit> = try {
        dao.reassignClass(SchoolClassAssignment(newSchoolId, classId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun getOrphanedClasses(): Result<List<ClassModel>> = try {
        Result.Success(dao.getOrphanedClasses().map { it.toDomain() })
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    override suspend fun updateClassSchool(classId: String, schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateClassSchool(classId, schoolId)
            dao.assignClass(SchoolClassAssignment(schoolId, classId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun approveSchool(schoolId: String): Result<Unit> = try {
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

    override suspend fun rejectSchool(schoolId: String, reason: String): Result<Unit> = try {
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

    override suspend fun pushSchool(schoolId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
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
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
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

    override suspend fun pushAccessRequests(accountId: String): Result<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val unsynced = accessRequestDao.getUnsyncedRequestsByAccount(accountId)
            if (unsynced.isEmpty()) return@withContext Result.Success(Unit)

            for (request in unsynced) {
                val requestData = mapOf(
                    "requestId" to request.requestId,
                    "accountId" to request.accountId,
                    "schoolId" to request.schoolId,
                    "schoolName" to request.schoolName,
                    "status" to request.status.name,
                    "createdAt" to request.createdAt,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )

                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("access_requests")
                        .document(request.requestId)
                        .set(requestData),
                )

                accessRequestDao.insertRequest(
                    request.copy(
                        syncStatus = SyncStatus.SYNCED,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
