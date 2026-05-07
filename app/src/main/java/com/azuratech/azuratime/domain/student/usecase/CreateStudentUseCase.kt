package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuraengine.model.StudentModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@Deprecated(
    message = "Use SaveStudentProfileUseCase. Migration: Replace with StudentProfile + SaveStudentProfileUseCase",
    replaceWith = ReplaceWith("SaveStudentProfileUseCase")
)
class CreateStudentUseCase @Inject constructor(
    private val saveStudentProfileUseCase: SaveStudentProfileUseCase,
    private val getUserByIdUseCase: com.azuratech.azuratime.domain.user.usecase.GetUserByIdUseCase,
    private val sessionManager: SessionManager,
    private val faceRemoteDataSource: FaceRemoteDataSource,
    private val photoStorageUtils: PhotoStorageUtils
) {

    suspend operator fun invoke(
        schoolId: String?,
        name: String,
        studentCode: String?,
        classId: String?,
        faceEmbedding: FloatArray?,
        photoBytes: ByteArray?,
        createdAtTimestamp: Long = System.currentTimeMillis()
    ): Result<StudentModel> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = sessionManager.getCurrentUserId()
            val user = currentUserId?.let { getUserByIdUseCase(it) }

            val resolvedSchoolId = schoolId 
                ?: sessionManager.getActiveSchoolId()
                ?: if (user?.role == "SUPER_ADMIN") {
                    return@withContext Result.Failure(AppError.BusinessRule("Please select a school first"))
                } else {
                    return@withContext Result.Failure(AppError.BusinessRule("School context required"))
                }

            if (resolvedSchoolId.isBlank()) {
                return@withContext Result.Failure(AppError.BusinessRule("School context is invalid (empty ID)"))
            }

            val studentId = "STU-${UUID.randomUUID().toString().take(8)}"
            val faceId = if (faceEmbedding != null) {
                "FACE-${studentId}-${System.currentTimeMillis()}"
            } else {
                "STUDENT_$studentId"
            }

            // 1. Photo Handling (Storage & Remote)
            var finalPhotoUrl: String? = null
            if (faceEmbedding != null && photoBytes != null) {
                finalPhotoUrl = photoStorageUtils.saveFacePhoto(photoBytes, faceId)

                val uploadResult = faceRemoteDataSource.uploadFacePhoto(resolvedSchoolId, faceId, photoBytes)
                if (uploadResult is Result.Success) {
                    finalPhotoUrl = uploadResult.data
                }
            }

            // 2. Map to StudentProfile Domain Model
            val profile = StudentProfile(
                studentId = studentId,
                studentCode = studentCode,
                name = name,
                schoolId = resolvedSchoolId,
                classIds = if (classId != null) listOf(classId) else emptyList(),
                faceId = faceId,
                embedding = faceEmbedding,
                photoUrl = finalPhotoUrl,
                syncStatus = SyncStatus.PENDING_UPDATE,
                createdAt = createdAtTimestamp
            )

            // 3. Save via SSOT UseCase
            val saveResult = saveStudentProfileUseCase(profile)
            
            if (saveResult is Result.Failure) {
                return@withContext Result.Failure(saveResult.error)
            }

            Result.Success(StudentModel(
                studentId = studentId,
                schoolId = resolvedSchoolId,
                name = name,
                studentCode = studentCode,
                classId = classId,
                createdAt = createdAtTimestamp,
                isSynced = false
            ))
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
