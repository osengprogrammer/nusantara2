package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * UseCase to get all classes for the active school.
 */
class GetClassesUseCase @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val classDao = database.classDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<ClassEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                classDao.observeClassesBySchool(schoolId)
                    .map { Result.Success(it) as Result<List<ClassEntity>> }
            }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
}
