package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏫 CLASS REPOSITORY - THE SINGLE SOURCE OF TRUTH
 */
@Singleton
class ClassRepository @Inject constructor(
    private val application: Application,
    private val database: AppDatabase,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {

    private val classDao = database.classDao()

    // =====================================================
    // 📖 1. REACTIVE READS
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val allClasses: Flow<List<ClassEntity>> = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            classDao.observeClassesBySchool(schoolId)
        }
        .flowOn(Dispatchers.IO)

    private val currentSchoolId: String
        get() = sessionManager.getActiveSchoolId() ?: ""

    // =====================================================
    // ✍️ 2. CREATE & UPDATE
    // =====================================================

    suspend fun upsertClass(name: String, id: String? = null) = withContext(Dispatchers.IO) {
        val schoolId = currentSchoolId
        if (schoolId.isEmpty()) return@withContext

        val classEntity = ClassEntity(
            id = id ?: UUID.randomUUID().toString(),
            name = name,
            schoolId = schoolId
        )

        classDao.insert(classEntity)

        try {
            syncClassToCloud(schoolId, classEntity)
        } catch (e: Exception) {
            Log.e("ClassRepository", "Cloud sync failed: ${e.message}")
        }
    }

    // =====================================================
    // 📥 3. BULK IMPORT & DELTA SYNC
    // =====================================================

    suspend fun performClassDeltaSync() = withContext(Dispatchers.IO) {
        val schoolId = currentSchoolId
        if (schoolId.isEmpty()) return@withContext

        try {
            val collectionRef = db.collection("schools").document(schoolId).collection("classes")
            
            // Fetch from tenant collection
            val snapshot = collectionRef.get().await()
            val classEntities = mutableListOf<ClassEntity>()

            if (snapshot.documents.isNotEmpty()) {
                classEntities.addAll(snapshot.documents.mapNotNull { doc ->
                    Log.e("AZURA_DEBUG", "Tenant Firestore Doc: ${doc.id} => ${doc.data}")
                    val data = doc.data
                    ClassEntity(
                        id = doc.id,
                        name = data?.get("name") as? String ?: "Unnamed Class",
                        schoolId = schoolId,
                        displayOrder = (data?.get("displayOrder") as? Number)?.toInt() ?: 0
                    )
                })
            }
            
            // FALLBACK / MIGRATION: Fetch from root "classes" collection
            try {
                val rootSnapshot = db.collection("classes").get().await()
                if (rootSnapshot.documents.isNotEmpty()) {
                    val rootClasses = rootSnapshot.documents.mapNotNull { doc ->
                        Log.e("AZURA_DEBUG", "Root Firestore Doc: ${doc.id} => ${doc.data}")
                        val data = doc.data
                        ClassEntity(
                            id = doc.id,
                            name = data?.get("name") as? String ?: "Unnamed Class",
                            schoolId = schoolId, // Assign to current tenant
                            displayOrder = (data?.get("displayOrder") as? Number)?.toInt() ?: 0
                        )
                    }
                    
                    // Filter out any that we already got from the tenant collection
                    val existingIds = classEntities.map { it.id }.toSet()
                    val newRootClasses = rootClasses.filter { it.id !in existingIds }
                    
                    if (newRootClasses.isNotEmpty()) {
                        classEntities.addAll(newRootClasses)
                        Log.i("ClassRepository", "✅ Migrated ${newRootClasses.size} classes from root collection")
                        
                        // Auto-migrate them to the correct tenant path in Firestore
                        newRootClasses.forEach { legacyClass ->
                            syncClassToCloud(schoolId, legacyClass)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ClassRepository", "Legacy root 'classes' collection fetch failed or not found: ${e.message}")
            }

            if (classEntities.isNotEmpty()) {
                // Get local classes to find any that were deleted on the server
                val localClasses = classDao.getClassesBySchoolOnce(schoolId)
                val serverIds = classEntities.map { it.id }.toSet()
                
                // Delete local classes that no longer exist on the server
                val classesToDelete = localClasses.filter { it.id !in serverIds }
                classesToDelete.forEach { classDao.deleteById(it.id) }

                // Insert or update server classes
                classDao.insertAll(classEntities)
                
                sessionManager.saveLastClassesSyncTime()
                Log.i("ClassRepository", "✅ Class Full Sync: Replaced/Updated ${classEntities.size} classes")
            }
        } catch (e: Exception) {
            Log.e("ClassRepository", "❌ Class Sync Failed: ${e.message}")
        }
    }

    suspend fun bulkImportClasses(data: List<Map<String, String>>) = withContext(Dispatchers.IO) {
        val schoolId = currentSchoolId
        if (schoolId.isEmpty()) return@withContext

        val classEntities = data.map { row ->
            ClassEntity(
                id = UUID.randomUUID().toString(),
                name = row["name"] ?: "Unnamed Class",
                schoolId = schoolId
            )
        }

        classDao.insertAll(classEntities)

        classEntities.forEach {
            try { syncClassToCloud(schoolId, it) } catch (e: Exception) {}
        }
    }

    // =====================================================
    // 🗑️ 4. DELETE
    // =====================================================

    suspend fun deleteClass(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = currentSchoolId
        if (schoolId.isEmpty()) return@withContext Result.failure(Exception("No active school"))

        val studentCount = classDao.getStudentCountForClass(schoolId, id)
        if (studentCount > 0) {
            return@withContext Result.failure(Exception("Gagal! Masih ada $studentCount siswa di kelas ini."))
        }

        classDao.deleteById(id)

        return@withContext try {
            deleteClassFromCloud(schoolId, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    // =====================================================
    // ☁️ CLOUD OPERATIONS (Migrated from FirestoreManager)
    // =====================================================

    suspend fun syncClassToCloud(schoolId: String, classEntity: ClassEntity) {
        val data = hashMapOf(
            "id" to classEntity.id,
            "name" to classEntity.name,
            "displayOrder" to classEntity.displayOrder,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        db.collection("schools").document(schoolId).collection("classes").document(classEntity.id).set(data, SetOptions.merge()).await()
    }

    suspend fun deleteClassFromCloud(schoolId: String, id: String) {
        db.collection("schools").document(schoolId).collection("classes").document(id).delete().await()
    }
}