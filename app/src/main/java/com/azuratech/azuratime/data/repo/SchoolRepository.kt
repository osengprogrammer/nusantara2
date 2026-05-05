package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.domain.model.AccessRequestStatus
import com.azuratech.azuratime.domain.model.SyncStatus
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val database: AppDatabase,
    private val remoteDataSource: SchoolRemoteDataSource,
    private val syncManager: SyncManager
) {
    private val dao = database.schoolClassDao()
    private val schoolDao = database.schoolDao()
    private val userDao = database.userDao()
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

                // Create initial AccessRequest (Approved for creator)
                val requestId = "req_creator_$schoolId"
                accessRequestDao.insertRequest(AccessRequestEntity(
                    requestId = requestId,
                    requesterId = adminId,
                    schoolId = schoolId,
                    schoolName = name,
                    status = AccessRequestStatus.APPROVED,
                    syncStatus = SyncStatus.PENDING_INSERT
                ))

                // Update User Memberships
                val user = userDao.getUserById(adminId)
                if (user != null) {
                    val updatedMemberships = user.memberships.toMutableMap()
                    updatedMemberships[schoolId] = Membership(
                        schoolName = name,
                        role = "ADMIN"
                    )
                    userDao.updateUser(user.copy(
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
        println("✅ DEBUG: School saved to Room: ${school.id} with status ${school.status}")
    }

    suspend fun getSchoolById(id: String): School? = 
        dao.getSchoolById(id)?.toDomain()

    suspend fun getCountByUser(accountId: String): Int = dao.getSchoolCountByAccount(accountId)

    suspend fun getFirstSchoolId(accountId: String): String? = dao.getFirstSchoolId(accountId)

    suspend fun schoolExists(schoolId: String): Boolean = dao.getSchoolById(schoolId) != null

    suspend fun deleteSchool(id: String, accountId: String): Result<Unit> = try {
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

    suspend fun saveClass(accountId: String, schoolId: String?, classModel: ClassModel): Result<Unit> {
        return try {
            val entity = ClassEntity(
                id = classModel.id,
                accountId = accountId,
                schoolId = schoolId, // Link if provided, otherwise independent
                name = classModel.name,
                grade = classModel.grade,
                teacherId = classModel.teacherId,
                studentCount = classModel.studentCount,
                createdAt = classModel.createdAt
            )
            dao.upsertClass(entity)
            println("✅ Repository: Saved class locally to Room -> ${classModel.id}")

            // If schoolId is provided, also create an assignment
            if (schoolId != null) {
                dao.assignClass(com.azuratech.azuratime.data.local.SchoolClassAssignment(schoolId, classModel.id))
            }

            // Async Sync to Remote
            repositoryScope.launch {
                // We still pass schoolId to remote if it exists, or handle as global class
                val remoteSchoolId = schoolId ?: "global"
                try {
                    remoteDataSource.saveClass(accountId, remoteSchoolId, classModel)
                    println("✅ Repository: Saved to Firestore -> schools/$remoteSchoolId/classes/${classModel.id}")
                } catch (e: Exception) {
                    println("❌ Repository: Failed to save to Firestore -> ${e.message}")
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    suspend fun assignClassToSchool(schoolId: String, classId: String): Result<Unit> = try {
        dao.assignClass(com.azuratech.azuratime.data.local.SchoolClassAssignment(schoolId, classId))
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

    suspend fun deleteClass(accountId: String, schoolId: String, classId: String): Result<Unit> {
        return try {
            val studentCount = dao.getStudentCountForClass(schoolId, classId)
            if (studentCount > 0) {
                return Result.Failure(AppError.BusinessRule("Gagal! Masih ada $studentCount siswa di kelas ini."))
            }

            dao.deleteClassById(classId)

            // Async Sync to Remote
            repositoryScope.launch {
                remoteDataSource.deleteClass(accountId, schoolId, classId)
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

    suspend fun reassignClass(accountId: String, classId: String, newSchoolId: String): Result<Unit> = try {
        dao.reassignClass(com.azuratech.azuratime.data.local.SchoolClassAssignment(newSchoolId, classId))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun getOrphanedClasses(): List<ClassModel> =
        dao.getOrphanedClasses().map { it.toDomain() }

    suspend fun updateClassSchool(classId: String, schoolId: String) {
        dao.updateClassSchool(classId, schoolId)
        dao.assignClass(com.azuratech.azuratime.data.local.SchoolClassAssignment(schoolId, classId))
    }
}
