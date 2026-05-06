package com.azuratech.azuratime.data.repo

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.StudentEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepositoryLegacyImpl @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) : StudentRepository {

    private val studentDao = database.studentDao()

    override suspend fun saveStudent(student: StudentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            studentDao.upsert(student)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun saveStudentToCloud(
        studentId: String,
        schoolId: String,
        data: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TODO: SSOT Migration v7.1 - Replace with Room-first + WorkManager sync
            db.collection("schools").document(schoolId)
                .collection("students").document(studentId)
                .set(data).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getStudentById(studentId: String, schoolId: String): StudentEntity? = withContext(Dispatchers.IO) {
        studentDao.getById(studentId, schoolId)
    }
}
