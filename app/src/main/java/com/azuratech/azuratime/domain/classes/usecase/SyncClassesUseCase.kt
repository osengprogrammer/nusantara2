package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncClassesUseCase @Inject constructor(
    private val repository: SchoolRepository,
    private val remoteDataSource: SchoolRemoteDataSource
) {
    suspend operator fun invoke(accountId: String, schoolId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteResult = remoteDataSource.getClasses(accountId, schoolId)
            if (remoteResult is Result.Success) {
                remoteResult.data.forEach { classModel ->
                    repository.saveClassLocally(
                        ClassEntity(
                            id = classModel.id,
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
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
