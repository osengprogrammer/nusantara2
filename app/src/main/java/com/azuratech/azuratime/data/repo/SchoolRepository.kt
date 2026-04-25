package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.SchoolEntity
import com.azuratech.azuratime.data.local.ClassEntity
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
        
        // Async Sync to Remote
        repositoryScope.launch {
            remoteDataSource.saveSchool(school.accountId, school)
        }
        
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.LocalDB(e.message))
    }

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

    // Class Operations
    suspend fun saveClassLocally(classEntity: ClassEntity) {
        dao.upsertClass(classEntity)
    }

    fun getLocalClasses(schoolId: String) = dao.getClasses(schoolId)
}
