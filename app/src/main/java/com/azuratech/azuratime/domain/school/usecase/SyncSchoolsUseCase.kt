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
    suspend operator fun invoke(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Sync Schools
            val remoteSchoolsResult = remoteDataSource.getSchools(accountId)
            if (remoteSchoolsResult is Result.Success) {
                remoteSchoolsResult.data.forEach { school ->
                    repository.saveSchool(school) // This will also trigger an async push back, but it's merge-based so fine
                }
            }

            // 2. Sync Classes for each school
            val localSchools = remoteSchoolsResult.getOrNull() ?: emptyList()
            localSchools.forEach { school ->
                val remoteClassesResult = remoteDataSource.getClasses(accountId, school.id)
                if (remoteClassesResult is Result.Success) {
                    remoteClassesResult.data.forEach { classModel ->
                        repository.saveClassLocally(
                            ClassEntity(
                                id = classModel.id,
                                accountId = accountId,
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
}
