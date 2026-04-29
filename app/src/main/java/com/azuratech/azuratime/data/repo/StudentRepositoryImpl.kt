package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.StudentEntity
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) : StudentRepository {

    private val studentDao = database.studentDao()

    override suspend fun saveStudent(student: StudentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            studentDao.upsert(student)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.DatabaseError(e.message ?: "Failed to save student locally"))
        }
    }

    override suspend fun saveStudentToCloud(
        studentId: String,
        schoolId: String,
        data: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            db.collection("schools").document(schoolId)
                .collection("students").document(studentId)
                .set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.RemoteError(e.message ?: "Failed to save student to cloud"))
        }
    }

    override suspend fun getStudentById(studentId: String): StudentEntity? = withContext(Dispatchers.IO) {
        studentDao.getStudentById(studentId)
    }
}
