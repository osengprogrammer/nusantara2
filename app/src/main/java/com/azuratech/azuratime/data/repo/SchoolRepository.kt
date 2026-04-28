package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.SchoolEntity
import com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
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
    private val remoteDataSource: SchoolRemoteDataSource
) {
    private val dao = database.schoolClassDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun observeSchools(accountId: String): Flow<Result<List<School>>> =
        dao.getSchools(accountId)
            .map { entities -> 
                Result.Success(entities.map { it.toDomain() }) as Result<List<School>>
            }
            .catch { e -> 
                emit(Result.Failure(AppError.LocalDB(e.message)))
            }

    suspend fun saveSchool(school: School): Result<Unit> = try {
        dao.upsertSchool(
            SchoolEntity(
                id = school.id,
                accountId = school.accountId,
                name = school.name,
                timezone = school.timezone,
                createdAt = school.createdAt,
                updatedAt = System.currentTimeMillis()
            )
        )
        println("✅ DEBUG: School saved to Room: ${school.id}")
        
        // Async Sync to Remote
        repositoryScope.launch {
            remoteDataSource.saveSchool(school.accountId, school)
        }
        
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

    suspend fun getSchoolById(id: String): School? = 
        dao.getSchoolById(id)?.toDomain()

    suspend fun getFirstSchoolId(accountId: String): String? = dao.getFirstSchoolId(accountId)

    suspend fun schoolExists(schoolId: String): Boolean = dao.getSchoolById(schoolId) != null

    suspend fun deleteSchool(id: String, accountId: String): Result<Unit> = try {
        dao.deleteSchoolById(id)
        
        // Async Sync to Remote
        repositoryScope.launch {
            remoteDataSource.deleteSchool(accountId, id)
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

            // If schoolId is provided, also create an assignment
            if (schoolId != null) {
                dao.assignClass(com.azuratech.azuratime.data.local.SchoolClassAssignment(schoolId, classModel.id))
            }

            // Async Sync to Remote
            repositoryScope.launch {
                // We still pass schoolId to remote if it exists, or handle as global class
                remoteDataSource.saveClass(accountId, schoolId ?: "global", classModel)
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
