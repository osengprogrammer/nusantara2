package com.azuratech.azuratime.features.school.data.repo

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.student.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.account.data.local.SchoolMembership as LocalSchoolMembership
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.domain.model.toAccountRole
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
    private val sessionManager: SessionManager, // 🔥 Added for account context
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
) : SchoolRepository {
    private val dao = database.schoolClassDao()
    private val schoolDao = database.schoolDao()
    private val accountDao = database.accountDao()
    private val classDao = database.classDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val accessRequestDao = database.accessRequestDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun observeSchoolsFlow(accountId: String): Flow<Result<List<School>>> =
        dao.getSchoolsFlow(accountId)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override fun observeSchoolsByIdsFlow(schoolIds: List<String>): Flow<Result<List<School>>> =
        dao.observeSchoolsByIdsFlow(schoolIds)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override fun observeSchoolByIdFlow(id: String): Flow<Result<School?>> =
        dao.observeSchoolByIdFlow(id)
            .map { it?.toDomain() }
            .asLocalResult()

    override fun observeAllSchoolsFlow(): Flow<Result<List<School>>> =
        dao.observeAllSchoolsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override suspend fun createSchool(adminId: String, name: String, timezone: String): Result<String> {
        return try {
            val account = accountDao.getAccountById(adminId)
                ?: return Result.Failure(AppError.LocalDB("Account not found."))

            // 🔥 AI Native: Check Membership Role (Owner/Admin in any school) or Global Role
            val isAdminInAnySchool = account.memberships.values.any { membership ->
                val mRole = membership.role.toAccountRole()
                mRole == AccountRole.ADMIN || mRole == AccountRole.SUPER_ADMIN
            }
            val globalRole = account.role.toAccountRole()
            val isGlobalAdmin = globalRole == AccountRole.ADMIN || globalRole == AccountRole.SUPER_ADMIN

            if (!isAdminInAnySchool && !isGlobalAdmin) {
                return Result.Failure(AppError.BusinessRule("Only Admins can create schools. Access denied."))
            }

            // 🔥 AI Native: Admin Multi-School Restriction
            if (globalRole == AccountRole.ADMIN) {
                val schoolCount = dao.getSchoolCountByAccount(adminId)
                if (schoolCount >= 1) {
                    return Result.Failure(AppError.BusinessRule("Admin limit reached (1 school max)."))
                }
            }

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

                val updatedMemberships = account.memberships.toMutableMap()
                updatedMemberships[schoolId] = LocalSchoolMembership(
                    schoolName = name,
                    role = "ADMIN",
                    status = "ACTIVE",
                    assignments = emptyList(),
                )
                accountDao.updateAccount(account.copy(memberships = updatedMemberships, activeSchoolId = schoolId))
            }
            // 🔥 Trigger immediate sync to Firestore
            syncManager.enqueueSchoolSync(schoolId)
            syncManager.enqueueAccessSync(adminId)

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
            when (remoteResult) {
                is Result.Success -> {
                    database.withTransaction {
                        // 🔥 AI Native: Ensure school exists locally. Do NOT auto-create "Ghost Schools".
                        if (dao.getSchoolById(schoolId) == null) {
                            return@withTransaction Result.Failure(AppError.LocalDB("School $schoolId not found locally. Sync aborted."))
                        }

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
                                isSynced = true, // 🔥 Since it's from remote, it's synced
                            )
                            dao.upsertClass(entity)
                            dao.assignClass(SchoolClassAssignment(schoolId, classModel.id))
                        }
                        Result.Success(Unit)
                    }
                    Result.Success(Unit)
                }
                is Result.Failure -> Result.Failure(remoteResult.error)
                else -> Result.Failure(AppError.LocalDB("Unknown sync state"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun deleteSchool(id: String, accountId: String): Result<Unit> {
        return try {
            val existing = dao.getSchoolById(id)
            if (existing != null) {
                database.withTransaction {
                    // 1. Mark school as deleted for sync
                    dao.upsertSchool(
                        existing.copy(
                            status = "DELETED",
                            syncStatus = SyncStatus.PENDING_DELETE.name,
                        ),
                    )

                    // 2. Remove from local account memberships
                    val account = accountDao.getAccountById(accountId)
                    if (account != null) {
                        val updatedMemberships = account.memberships.toMutableMap()
                        updatedMemberships.remove(id)

                        // If it was the active school, clear it
                        val activeSchoolId = if (account.activeSchoolId == id) null else account.activeSchoolId

                        accountDao.updateAccount(
                            account.copy(
                                memberships = updatedMemberships,
                                activeSchoolId = activeSchoolId,
                            ),
                        )
                    }
                }
                syncManager.enqueueSchoolSync(id)
                syncManager.enqueueAccessSync(accountId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun observeClassesFlow(schoolId: String): Flow<Result<List<ClassModel>>> =
        dao.getClassesFlow(schoolId)
            .map { entities -> entities.map { it.toDomain() } }
            .asLocalResult()

    override suspend fun getClasses(schoolId: String): Result<List<ClassModel>> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getClassesFlow(schoolId).first()
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveClass(_accountId: String, schoolId: String?, classModel: ClassModel): Result<Unit> {
        return try {
            database.withTransaction {
                val schoolIdVal = schoolId
                if (schoolIdVal != null) {
                    // 🔥 AI Native: Ensure school exists locally. Do NOT auto-create "Ghost Schools".
                    if (dao.getSchoolById(schoolIdVal) == null) {
                        return@withTransaction Result.Failure(AppError.LocalDB("School $schoolIdVal not found locally. Assignment aborted."))
                    }

                    // Check if class with same name already exists in this school
                    val existing = dao.getClassByNameAndSchool(schoolIdVal, classModel.name)
                    if (existing != null) {
                        val updated = existing.copy(
                            grade = classModel.grade,
                            accountId = classModel.accountId,
                            studentCount = classModel.studentCount,
                        )
                        classDao.update(updated)
                        dao.assignClass(SchoolClassAssignment(schoolIdVal, existing.id))
                        return@withTransaction Result.Success(Unit)
                    }
                }

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

                if (schoolIdVal != null) {
                    dao.assignClass(SchoolClassAssignment(schoolIdVal, classModel.id))
                }
                Result.Success(Unit)
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
        database.withTransaction {
            dao.assignClass(SchoolClassAssignment(schoolId, classId))
            dao.updateClassSchool(classId, schoolId)
        }
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
                return Result.Failure(AppError.BusinessRule("Failed! There are still $studentCount students in this class."))
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

    override fun getLocalClassesFlow(schoolId: String): Flow<Result<List<ClassEntity>>> =
        dao.getClassesFlow(schoolId)
            .asLocalResult()

    override fun observeAllClassesForAccountFlow(accountId: String): Flow<Result<List<ClassModel>>> =
        dao.getAllClassesFlow(accountId).map { entities ->
            entities.map { it.toDomain() }
        }.asLocalResult()

    override suspend fun reassignClass(accountId: String, classId: String, newSchoolId: String): Result<Unit> = try {
        database.withTransaction {
            // 🔥 AI Native: Ensure school exists locally. Do NOT auto-create "Ghost Schools".
            if (dao.getSchoolById(newSchoolId) == null) {
                return@withTransaction Result.Failure(AppError.LocalDB("Target School $newSchoolId not found locally. Reassignment aborted."))
            }
            dao.reassignClass(SchoolClassAssignment(newSchoolId, classId))
            Result.Success(Unit)
        }
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
            database.withTransaction {
                // 🔥 AI Native: Ensure school exists locally. Do NOT auto-create "Ghost Schools".
                if (dao.getSchoolById(schoolId) == null) {
                    return@withTransaction Result.Failure(AppError.LocalDB("School $schoolId not found locally. Update aborted."))
                }
                dao.updateClassSchool(classId, schoolId)
                dao.assignClass(SchoolClassAssignment(schoolId, classId))
                Result.Success(Unit)
            }
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
                if (school.status == "DELETED") {
                    // 🔥 Perform actual deletion from BOTH sources
                    val remoteResult = remoteDataSource.deleteSchool(school.accountId, school.id)
                    if (remoteResult is Result.Success) {
                        dao.deleteSchoolById(school.id)
                    }
                } else {
                    val remoteResult = remoteDataSource.saveSchool(school.accountId, school.toDomain())
                    if (remoteResult is Result.Success) {
                        dao.upsertSchool(school.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
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

    override suspend fun addStudentToClass(schoolId: String, classId: String, studentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Update Local Room DB
            assignmentDao.insertAssignment(
                StudentClassAssignmentEntity(
                    studentId = studentId,
                    classId = classId,
                    schoolId = schoolId,
                    isSynced = false, // Will be synced by push if we had a worker for it, but for now we push direct
                ),
            )

            // 2. Update Remote Firebase
            remoteDataSource.addStudentToClass(schoolId, classId, studentId)
                .onSuccess {
                    assignmentDao.updateSyncStatus(studentId, classId, schoolId, true)
                }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun saveGeofence(
        schoolId: String,
        lat: Double,
        lng: Double,
        radius: Int,
        isActive: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val geofenceId = "geo_$schoolId"
            val entity = GpsGeofenceEntity(
                id = geofenceId,
                schoolId = schoolId,
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
                isActive = isActive,
                syncStatus = SyncStatus.PENDING_UPDATE.name,
            )
            dao.upsertGeofence(entity)

            // 🔥 AI Native: Sync to Firestore SSOT
            val remoteResult = remoteDataSource.saveGeofence(schoolId, entity)
            if (remoteResult is Result.Success) {
                dao.upsertGeofence(entity.copy(syncStatus = SyncStatus.SYNCED.name))
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun observeGeofenceFlow(schoolId: String): Flow<GpsGeofenceEntity?> =
        dao.observeGeofenceFlow(schoolId)
}
