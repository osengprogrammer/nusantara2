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

class CreateStudentUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val faceRemoteDataSource: FaceRemoteDataSource,
    private val photoStorageUtils: PhotoStorageUtils
) {
    private val studentDao = database.studentDao()
    private val faceDao = database.faceDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    suspend operator fun invoke(
        schoolId: String?,
        name: String,
        studentCode: String?,
        classId: String?,
        faceEmbedding: FloatArray?,
        photoBytes: ByteArray?
    ): Result<StudentModel> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = sessionManager.getCurrentUserId()
            val user = currentUserId?.let { database.userDao().getUserById(it) }

            val resolvedSchoolId = schoolId 
                ?: sessionManager.getActiveSchoolId()
                ?: if (user?.role == "SUPER_ADMIN") {
                    return@withContext Result.Failure(AppError.BusinessRule("Please select a school first"))
                } else {
                    return@withContext Result.Failure(AppError.BusinessRule("School context required"))
                }

            println("🔍 CreateStudent: resolvedSchoolId=$resolvedSchoolId for user ${user?.userId}")

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
            val studentData = hashMapOf(
                "studentId" to studentId,
                "schoolId" to resolvedSchoolId,
                "name" to name,
                "studentCode" to studentCode,
                "classId" to classId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            db.collection("schools").document(resolvedSchoolId)
                .collection("students").document(studentId)
                .set(studentData, SetOptions.merge()).await()

            // 2. Local Save Student
            studentDao.upsert(studentEntity.copy(isSynced = true))

            // 3. Handle Face if provided
            if (faceEmbedding != null) {
                val faceId = "FACE-${studentId}-${System.currentTimeMillis()}"
                
                var finalPhotoUrl: String? = photoBytes?.let {
                    photoStorageUtils.saveFacePhoto(it, faceId)
                }

                photoBytes?.let { bytes ->
                    val uploadResult = faceRemoteDataSource.uploadFacePhoto(resolvedSchoolId, faceId, bytes)
                    if (uploadResult is Result.Success) {
                        finalPhotoUrl = uploadResult.data
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
                
                // Sync Face to Cloud
                faceRemoteDataSource.bulkSyncFaces(resolvedSchoolId, listOf(faceEntity))
                faceDao.upsertFace(faceEntity.copy(isSynced = true))

                // Handle Assignment
                if (classId != null) {
                    val assignment = FaceAssignmentEntity(
                        faceId = faceId,
                        classId = classId,
                        schoolId = resolvedSchoolId,
                        isSynced = false
                    )
                    faceRemoteDataSource.syncFaceAssignment(assignment)
                    faceAssignmentDao.insertAssignment(assignment.copy(isSynced = true))
                }
                println("🔗 DEBUG: Created Student $studentId, linked face: true")
            } else {
                println("🔗 DEBUG: Created Student $studentId, linked face: false")
            }

            Result.Success(studentEntity.toDomain())

        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
