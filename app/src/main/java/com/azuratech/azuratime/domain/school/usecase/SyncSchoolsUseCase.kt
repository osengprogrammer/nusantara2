package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.SchoolEntity
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncSchoolsUseCase @Inject constructor(
    private val repository: SchoolRepository,
    private val remoteDataSource: SchoolRemoteDataSource
) {
    suspend operator fun invoke(schoolIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Sync Schools from Global Collection
            val remoteSchoolsResult = remoteDataSource.getSchoolsByIds(schoolIds)
            if (remoteSchoolsResult is Result.Success) {
                remoteSchoolsResult.data.forEach { school ->
                    repository.saveSchoolLocally(school)
                }
            }

            // 2. Sync Classes for each school
            val localSchools = remoteSchoolsResult.getOrNull() ?: emptyList()
            localSchools.forEach { school ->
                val remoteClassesResult = remoteDataSource.getClasses(school.accountId, school.id)
                if (remoteClassesResult is Result.Success) {
                    remoteClassesResult.data.forEach { classModel ->
                        repository.saveClassLocally(
                            ClassEntity(
                                id = classModel.id,
                                accountId = school.accountId,
                                schoolId = classModel.schoolId,
                                name = classModel.name,
                                grade = classModel.grade,
                                teacherId = classModel.teacherId,
                                studentCount = classModel.studentCount,
                                createdAt = classModel.createdAt
                            )
                        )
                    }
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    /** Legacy support if needed */
    suspend fun syncAllForAccount(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val result = remoteDataSource.getSchools(accountId)
        if (result is Result.Success) {
            invoke(result.data.map { it.id })
        } else {
            Result.Failure((result as Result.Failure).error)
        }
    }
}
