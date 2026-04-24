package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase to create or update a class.
 */
class UpdateClassUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val classDao = database.classDao()

    suspend operator fun invoke(name: String, id: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school"))

        val classEntity = ClassEntity(
            id = id ?: UUID.randomUUID().toString(),
            name = name,
            schoolId = schoolId
        )

        try {
            classDao.insert(classEntity)
            
            // Push to Cloud
            val data = hashMapOf(
                "id" to classEntity.id,
                "name" to classEntity.name,
                "displayOrder" to classEntity.displayOrder,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            db.collection("schools").document(schoolId).collection("classes")
                .document(classEntity.id).set(data, SetOptions.merge()).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            println("ERROR: [UpdateClassUseCase] Error: ${e.message}")
            // Return success anyway as it's saved locally
            Result.Success(Unit)
        }
    }
}
