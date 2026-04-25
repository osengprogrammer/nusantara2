package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to synchronize classes from Firestore to local Room DB.
 */
class SyncClassesUseCase @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val classDao = database.classDao()

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        try {
            val collectionRef = db.collection("schools").document(schoolId).collection("classes")
            
            // 1. Fetch from tenant collection
            val snapshot = collectionRef.get().await()
            val classEntities = mutableListOf<ClassEntity>()

            if (snapshot.documents.isNotEmpty()) {
                classEntities.addAll(snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    ClassEntity(
                        id = doc.id,
                        name = data?.get("name") as? String ?: "Unnamed Class",
                        schoolId = schoolId,
                        displayOrder = (data?.get("displayOrder") as? Number)?.toInt() ?: 0
                    )
                })
            }
            
            // 2. FALLBACK / MIGRATION: Fetch from root "classes" collection
            try {
                val rootSnapshot = db.collection("classes").get().await()
                if (rootSnapshot.documents.isNotEmpty()) {
                    val rootClasses = rootSnapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        ClassEntity(
                            id = doc.id,
                            name = data?.get("name") as? String ?: "Unnamed Class",
                            schoolId = schoolId,
                            displayOrder = (data?.get("displayOrder") as? Number)?.toInt() ?: 0
                        )
                    }
                    
                    val existingIds = classEntities.map { it.id }.toSet()
                    val newRootClasses = rootClasses.filter { it.id !in existingIds }
                    
                    if (newRootClasses.isNotEmpty()) {
                        classEntities.addAll(newRootClasses)
                        println("[SyncClassesUseCase] ✅ Migrated ${newRootClasses.size} classes from root collection")
                        
                        // Auto-migrate them to the correct tenant path
                        newRootClasses.forEach { legacyClass ->
                            syncClassToCloud(schoolId, legacyClass)
                        }
                    }
                }
            } catch (e: Exception) {
                println("[SyncClassesUseCase] Legacy root fetch failed: ${e.message}")
            }

            // 3. Merge with local DB
            if (classEntities.isNotEmpty()) {
                val localClasses = classDao.getClassesBySchoolOnce(schoolId)
                val serverIds = classEntities.map { it.id }.toSet()
                
                // Delete local classes that no longer exist on server
                val classesToDelete = localClasses.filter { it.id !in serverIds }
                classesToDelete.forEach { classDao.deleteById(it.id) }

                // Insert or update
                classDao.insertAll(classEntities)
                
                sessionManager.saveLastClassesSyncTime()
                println("[SyncClassesUseCase] ✅ Class Full Sync: Updated ${classEntities.size} classes")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    private suspend fun syncClassToCloud(schoolId: String, classEntity: ClassEntity) {
        val data = hashMapOf(
            "id" to classEntity.id,
            "name" to classEntity.name,
            "displayOrder" to classEntity.displayOrder,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        db.collection("schools").document(schoolId).collection("classes").document(classEntity.id)
            .set(data, SetOptions.merge()).await()
    }
}
