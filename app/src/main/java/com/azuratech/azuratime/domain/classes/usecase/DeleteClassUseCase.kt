package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to delete a class.
 */
class DeleteClassUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val classDao = database.classDao()

    suspend operator fun invoke(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school"))

        val studentCount = classDao.getStudentCountForClass(schoolId, id)
        if (studentCount > 0) {
            return@withContext Result.Failure(AppError.BusinessRule("Gagal! Masih ada $studentCount siswa di kelas ini."))
        }

        classDao.deleteById(id)

        try {
            db.collection("schools").document(schoolId).collection("classes").document(id).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            // Success anyway as it's deleted locally
            Result.Success(Unit)
        }
    }
}
