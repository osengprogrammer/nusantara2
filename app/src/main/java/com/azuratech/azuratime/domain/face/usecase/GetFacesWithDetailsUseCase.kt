package com.azuratech.azuratime.domain.face.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.local.FaceWithDetails
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * UseCase to get all faces with their class details for a specific school.
 */
class GetFacesWithDetailsUseCase @Inject constructor(
    private val localDataSource: FaceLocalDataSource,
    private val sessionManager: SessionManager
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<FaceWithDetails>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                combine(
                    localDataSource.getAllFacesFlow(schoolId),
                    localDataSource.observeClassesBySchool(schoolId),
                    localDataSource.getAllAssignmentsFlow(schoolId)
                ) { faces, classes, assignments ->
                    val classMap = classes.associateBy { it.id }
                    val assignmentMap = assignments.groupBy { it.faceId }

                    val detailedFaces = faces.map { face ->
                        val userAssignments = assignmentMap[face.faceId] ?: emptyList()
                        val classNames = userAssignments.mapNotNull { classMap[it.classId]?.name }.joinToString(", ")

                        FaceWithDetails(
                            face = face,
                            className = classNames.ifEmpty { null },
                            classId = userAssignments.firstOrNull()?.classId
                        )
                    }.sortedBy { it.face.name }

                    Result.Success(detailedFaces) as Result<List<FaceWithDetails>>
                }
            }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
}
