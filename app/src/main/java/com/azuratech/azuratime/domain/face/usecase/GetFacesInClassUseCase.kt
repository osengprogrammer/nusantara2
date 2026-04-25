package com.azuratech.azuratime.domain.face.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * UseCase to get all faces assigned to a specific class.
 */
class GetFacesInClassUseCase @Inject constructor(
    private val localDataSource: FaceLocalDataSource,
    private val sessionManager: SessionManager
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(classId: String): Flow<Result<List<FaceEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getFacesInClassFlow(classId, schoolId)
            }
            .map { Result.Success(it) as Result<List<FaceEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
}
