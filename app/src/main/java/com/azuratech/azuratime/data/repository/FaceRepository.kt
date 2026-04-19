package com.azuratech.azuratime.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.face.usecase.*
import com.azuratech.azuratime.domain.face.RegisterResult
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: FaceLocalDataSource,
    private val remoteDataSource: FaceRemoteDataSource,
    private val sessionManager: SessionManager,
    private val registerFaceUseCase: RegisterFaceUseCase,
    private val syncFacesUseCase: SyncFacesUseCase,
    private val deleteFaceUseCase: DeleteFaceUseCase,
    private val updateFaceWithPhotoUseCase: UpdateFaceWithPhotoUseCase
) {
    private val schoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    @Deprecated("Route through GetFacesWithDetailsUseCase", ReplaceWith("getFacesWithDetailsUseCase()"))
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allFacesWithDetailsFlow: Flow<Result<List<FaceWithDetails>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                kotlinx.coroutines.flow.combine(
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

    @Deprecated("Route through GetFacesForScanningUseCase")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val facesForScanningFlow: Flow<Result<List<FaceEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getAllFacesForScanningFlow(schoolId)
            }
            .map { Result.Success(it) as Result<List<FaceEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    @Deprecated("Route through GetFacesInClassUseCase")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFacesInClassFlow(classId: String): Flow<Result<List<FaceEntity>>> =
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                localDataSource.getFacesInClassFlow(classId, schoolId)
            }
            .map { Result.Success(it) as Result<List<FaceEntity>> }
            .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    @Deprecated("Route through GetFaceWithDetailsUseCase")
    suspend fun getFaceWithDetails(faceId: String): Result<FaceWithDetails?> = withContext(Dispatchers.IO) {
        try {
            Result.Success(localDataSource.getFaceWithDetails(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through GetAssignmentsForFaceUseCase")
    suspend fun getAssignmentsForFace(faceId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.Success(localDataSource.getClassIdsForFace(faceId, schoolId))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through SyncFacesUseCase")
    suspend fun performFaceDeltaSync(): Result<Unit> = syncFacesUseCase()

    @Deprecated("Route through RegisterFaceUseCase")
    suspend fun registerFace(
        inputId: String,
        classId: String,
        name: String,
        embedding: FloatArray,
        photoBitmap: Bitmap?
    ): Result<RegisterResult> = registerFaceUseCase(inputId, classId, name, embedding, photoBitmap)

    @Deprecated("Route through DeleteFaceUseCase")
    suspend fun deleteFace(face: FaceEntity): Result<Unit> = deleteFaceUseCase(face.faceId)

    @Deprecated("Route through UpdateFaceUseCase")
    suspend fun updateEmployeeClass(faceId: String, newClassId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localDataSource.deleteAssignmentsByFace(faceId, schoolId)
            if (newClassId != null) {
                val assignment = FaceAssignmentEntity(faceId = faceId, classId = newClassId, schoolId = schoolId, isSynced = false)
                localDataSource.insertAssignment(assignment)
                val syncRes = remoteDataSource.syncFaceAssignment(assignment)
                if (syncRes is Result.Failure) throw Exception("Sync failed")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UpdateFaceUseCase")
    suspend fun updateFaceBasic(face: FaceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatedFace = face.copy(lastUpdated = System.currentTimeMillis(), isSynced = false)
            localDataSource.upsertFace(updatedFace)

            val syncRes = remoteDataSource.bulkSyncFaces(schoolId, listOf(updatedFace))
            if (syncRes is Result.Failure) throw Exception("Sync failed")

            localDataSource.upsertFace(updatedFace.copy(isSynced = true))
            FaceCache.refresh(application, schoolId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UpdateFaceWithPhotoUseCase")
    suspend fun updateFaceWithPhoto(face: FaceEntity, photoBitmap: Bitmap?, embedding: FloatArray): Result<Unit> =
        updateFaceWithPhotoUseCase(face, photoBitmap, embedding)

    @Deprecated("Route through UseCase layer")
    suspend fun uploadFacePhotoToCloud(schoolId: String, faceId: String, imageBytes: ByteArray): Result<String?> =
        remoteDataSource.uploadFacePhoto(schoolId, faceId, imageBytes)

    @Deprecated("Route through UseCase layer")
    suspend fun bulkSyncFacesToCloud(schoolId: String, faces: List<FaceEntity>): Result<Unit> =
        remoteDataSource.bulkSyncFaces(schoolId, faces)

    @Deprecated("Route through UseCase layer")
    suspend fun syncFaceAssignmentToCloud(assignment: FaceAssignmentEntity): Result<Unit> =
        remoteDataSource.syncFaceAssignment(assignment)
}
