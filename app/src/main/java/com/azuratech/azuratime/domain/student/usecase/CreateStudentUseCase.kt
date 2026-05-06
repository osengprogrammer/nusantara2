package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuraengine.model.StudentModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@Deprecated(
    message = "Use SaveStudentProfileUseCase. Migration: Replace with StudentProfile + SaveStudentProfileUseCase",
    replaceWith = ReplaceWith("SaveStudentProfileUseCase")
)
class CreateStudentUseCase @Inject constructor(
    private val studentRepository: com.azuratech.azuratime.data.repo.StudentRepository,
    private val getUserByIdUseCase: com.azuratech.azuratime.domain.user.usecase.GetUserByIdUseCase,
    private val sessionManager: SessionManager,
    private val faceRemoteDataSource: FaceRemoteDataSource,
    private val photoStorageUtils: PhotoStorageUtils,
    private val database: AppDatabase
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

            println("🔍 CreateStudent: resolvedSchoolId=$resolvedSchoolId for user ${user?.userId}")

            if (resolvedSchoolId.isBlank()) {
                return@withContext Result.Failure(AppError.BusinessRule("School context is invalid (empty ID)"))
            }

            val studentId = "STU-${UUID.randomUUID().toString().take(8)}"
            
            val studentEntity = StudentEntity(
                studentId = studentId,
                schoolId = resolvedSchoolId,
                name = name,
                studentCode = studentCode,
                classId = classId,
                isSynced = false
            )

            // 1. Save Student to Cloud
            val studentData: Map<String, Any> = mapOf(
                "studentId" to studentId,
                "schoolId" to resolvedSchoolId,
                "name" to name,
                "studentCode" to (studentCode ?: ""),
                "classId" to (classId ?: ""),
                "createdAt" to createdAtTimestamp
            )
            
            println("🔄 UseCase: Calling Repository.saveStudentToCloud for $resolvedSchoolId")
            val cloudResult = studentRepository.saveStudentToCloud(studentId, resolvedSchoolId, studentData)

            // 2. Local Save Student (Offline-first: always save locally)
            val isSynced = cloudResult is Result.Success
            val localSaveResult = studentRepository.saveStudent(studentEntity.copy(isSynced = isSynced))
            
            if (localSaveResult is Result.Failure) {
                println("❌ UseCase: Local save failed -> ${localSaveResult.error}")
                return@withContext Result.Failure(localSaveResult.error)
            }

            // 3. Always ensure a Face and Assignment record exists (Crucial for UI JOIN queries)
            val faceId = if (faceEmbedding != null) {
                "FACE-${studentId}-${System.currentTimeMillis()}"
            } else {
                "STUDENT_$studentId"
            }

            var finalPhotoUrl: String? = null
            if (faceEmbedding != null) {
                finalPhotoUrl = photoBytes?.let {
                    photoStorageUtils.saveFacePhoto(it, faceId)
                }

                photoBytes?.let { bytes ->
                    val uploadResult = faceRemoteDataSource.uploadFacePhoto(resolvedSchoolId, faceId, bytes)
                    if (uploadResult is Result.Success) {
                        finalPhotoUrl = uploadResult.data
                    }
                }
            }

            val faceEntity = FaceEntity(
                faceId = faceId,
                studentId = studentId,
                schoolId = resolvedSchoolId,
                name = name,
                photoUrl = finalPhotoUrl,
                embedding = faceEmbedding,
                isSynced = false
            )
            
            // 🔥 LOCAL SAVE (Crucial for scanner/offline/UI visibility)
            database.faceDao().upsertFace(faceEntity)

            // 🔥 ALWAYS POPULATE ASSIGNMENTS (For Class labels in UI)
            database.faceAssignmentDao().insertAssignment(
                FaceAssignmentEntity(
                    faceId = faceId,
                    classId = classId ?: "",
                    schoolId = resolvedSchoolId,
                    isSynced = false
                )
            )
            println("✅ Created face_assignment for student $studentId via faceId=$faceId")

            // Sync Face to Cloud if biometric exists
            if (faceEmbedding != null) {
                faceRemoteDataSource.bulkSyncFaces(resolvedSchoolId, listOf(faceEntity))
            }

            if (cloudResult is Result.Failure) {
                println("⚠️ UseCase: Cloud save failed but local succeeded -> ${cloudResult.error}")
            }

            Result.Success(studentEntity.toDomain())
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
