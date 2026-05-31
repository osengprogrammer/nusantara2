package com.azuratech.azuratime.features.school.data.repo

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.account.data.local.SchoolMembership as LocalSchoolMembership
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import kotlinx.coroutines.tasks.await
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
    private val classDao = database.classDao()
    private val accessRequestDao = database.accessRequestDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun observeSchools(accountId: String): Flow<Result<List<School>>> =
        dao.getSchools(accountId)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override fun observeSchoolsByIds(schoolIds: List<String>): Flow<Result<List<School>>> =
        dao.observeSchoolsByIds(schoolIds)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override fun observeSchoolById(id: String): Flow<Result<School?>> =
        dao.observeSchoolById(id)
            .map { it?.toDomain() }
            .asLocalResult()

    override fun observeAllSchools(): Flow<Result<List<School>>> =
        dao.observeAllSchools()
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

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
                    updatedMemberships[schoolId] = LocalSchoolMembership(
                        schoolName = name,
                        role = "ADMIN",
                        status = "ACTIVE",
                        assignedClassIds = emptyList(),
                    )
                    database.accountDao().updateAccount(user.copy(memberships = updatedMemberships, activeSchoolId = schoolId))
                }
            }
            Result.Success(schoolId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateSchoolDetails(schoolId: String, name: String?, timezone: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val school = dao.getSchoolById(schoolId)
            if (school != null) {
                dao.upsertSchool(
                    school.copy(
                        name = name ?: school.name,
                        timezone = timezone ?: school.timezone,
                        syncStatus = SyncStatus.PENDING_UPDATE.name,
                    ),
                )
                syncManager.enqueueSchoolSync(schoolId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveSchool(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.upsertSchool(SchoolEntity.fromDomain(school))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveSchoolLocally(school: School): Result<Unit> = withContext(Dispatchers.IO) {
        saveSchool(school)
    }

    override suspend fun getSchoolById(id: String): Result<School> = withContext(Dispatchers.IO) {
        try {
            val school = dao.getSchoolById(id)
            if (school != null) {
                Result.Success(school.toDomain())
            } else {
                Result.Failure(AppError.LocalDB("School not found"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getCountByAccount(accountId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Result.Success(dao.getSchoolCountByAccount(accountId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getFirstSchoolId(accountId: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            Result.Success(dao.getFirstSchoolId(accountId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun schoolExists(schoolId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Result.Success(dao.getSchoolById(schoolId) != null)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncSchools(schoolIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (schoolIds.isEmpty()) return@withContext Result.Success(Unit)

            val remoteResult = remoteDataSource.getSchoolsByIds(schoolIds)
            if (remoteResult is Result.Success) {
                remoteResult.data.forEach { school ->
                    dao.upsertSchool(SchoolEntity.fromDomain(school))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncClasses(accountId: String, schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteResult = remoteDataSource.getClasses(accountId, schoolId)
            if (remoteResult is Result.Success) {
                remoteResult.data.forEach { classModel ->
                    val entity = ClassEntity(
                        id = classModel.id,
                        ownerAccountId = accountId,
                        schoolId = schoolId,
                        name = classModel.name,
                        grade = classModel.grade,
                        accountId = classModel.accountId,
                        studentCount = classModel.studentCount,
                        createdAt = classModel.createdAt,
                    )
                    dao.upsertClass(entity)
                    dao.assignClass(SchoolClassAssignment(schoolId, classModel.id))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun deleteSchool(id: String, accountId: String): Result<Unit> {
        return try {
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
    }

    override fun observeClasses(schoolId: String): Flow<Result<List<ClassModel>>> =
        dao.getClasses(schoolId)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

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
            .asLocalResult()

    override fun observeAllClassesForAccount(accountId: String): Flow<Result<List<ClassModel>>> =
        dao.getAllClasses(accountId).map { entities ->
            entities.map { it.toDomain() }
        }.asLocalResult()

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

    override suspend fun pushSchool(schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val school = dao.getSchoolById(schoolId)
            if (school != null) {
                val remoteResult = remoteDataSource.saveSchool(school.accountId, school.toDomain())
                if (remoteResult is Result.Success) {
                    dao.upsertSchool(school.copy(syncStatus = SyncStatus.SYNCED.name))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun pushAccessRequests(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unsynced = accessRequestDao.getUnsyncedRequestsByAccount(accountId)
            unsynced.forEach { request ->
                val data = mapOf(
                    "requestId" to request.requestId,
                    "accountId" to request.accountId,
                    "schoolId" to request.schoolId,
                    "schoolName" to request.schoolName,
                    "status" to request.status.name,
                    "createdAt" to request.createdAt,
                    "updatedAt" to request.updatedAt,
                )
                firestore.collection("access_requests").document(request.requestId)
                    .set(data, com.google.firebase.firestore.SetOptions.merge()).await()

                accessRequestDao.insertRequest(request.copy(syncStatus = SyncStatus.SYNCED))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getClassById(id: String): Result<ClassModel?> = withContext(Dispatchers.IO) {
        try {
            Result.Success(classDao.getClassById(id)?.toDomain())
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
