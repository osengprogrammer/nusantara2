package com.azuratech.azuratime.data.remote

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : SchoolRemoteDataSource {

    private fun getAccountRef(accountId: String) = db.collection("accounts").document(accountId)
    private fun getSchoolsRef(accountId: String) = getAccountRef(accountId).collection("schools")
    
    // 🔥 Top-level collection for school discovery
    private fun getGlobalSchoolsRef() = db.collection("schools")

    private fun getClassesRef(accountId: String, schoolId: String) = 
        getGlobalSchoolsRef().document(schoolId).collection("classes")

    override suspend fun saveSchool(accountId: String, school: School): Result<Unit> {
        return try {
            val data = hashMapOf(
                "id" to school.id,
                "accountId" to school.accountId,
                "name" to school.name,
                "timezone" to school.timezone,
                "status" to school.status,
                "createdAt" to school.createdAt,
                "updatedAt" to school.updatedAt
            )
            // Save to global collection
            getGlobalSchoolsRef().document(school.id).set(data, SetOptions.merge()).await()
            // Backward compatibility: also save to account subcollection if needed
            getSchoolsRef(accountId).document(school.id).set(data, SetOptions.merge()).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteSchool(accountId: String, schoolId: String): Result<Unit> {
        return try {
            getGlobalSchoolsRef().document(schoolId).delete().await()
            getSchoolsRef(accountId).document(schoolId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun observeRemoteSchools(accountId: String): Flow<Result<List<School>>> = callbackFlow {
        // Query by accountId in the global collection
        val subscription = getGlobalSchoolsRef()
            .whereEqualTo("accountId", accountId)
            .addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.Failure(AppError.Network(error.message)))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val schools = snapshot.documents.mapNotNull { doc ->
                    try {
                        School(
                            id = doc.id,
                            accountId = doc.getString("accountId") ?: "",
                            name = doc.getString("name") ?: doc.getString("schoolName") ?: "",
                            timezone = doc.getString("timezone") ?: "UTC",
                            status = doc.getString("status") ?: "ACTIVE",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                trySend(Result.Success(schools))
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun getSchools(accountId: String): Result<List<School>> {
        return try {
            val snapshot = getGlobalSchoolsRef()
                .whereEqualTo("accountId", accountId)
                .get().await()
                
            val schools = snapshot.documents.mapNotNull { doc ->
                try {
                    School(
                        id = doc.id,
                        accountId = doc.getString("accountId") ?: "",
                        name = doc.getString("name") ?: doc.getString("schoolName") ?: "",
                        timezone = doc.getString("timezone") ?: "UTC",
                        status = doc.getString("status") ?: "ACTIVE",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) { null }
            }
            Result.Success(schools)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getSchoolsByIds(schoolIds: List<String>): Result<List<School>> {
        return try {
            if (schoolIds.isEmpty()) return Result.Success(emptyList())
            
            val snapshot = getGlobalSchoolsRef()
                .whereIn("id", schoolIds)
                .get().await()
                
            val schools = snapshot.documents.mapNotNull { doc ->
                try {
                    School(
                        id = doc.id,
                        accountId = doc.getString("accountId") ?: "",
                        name = doc.getString("name") ?: doc.getString("schoolName") ?: "",
                        timezone = doc.getString("timezone") ?: "UTC",
                        status = doc.getString("status") ?: "ACTIVE",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) { null }
            }
            Result.Success(schools)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun saveClass(accountId: String, schoolId: String, classModel: ClassModel): Result<Unit> {
        return try {
            val data = hashMapOf(
                "id" to classModel.id,
                "schoolId" to classModel.schoolId,
                "name" to classModel.name,
                "grade" to classModel.grade,
                "teacherId" to classModel.teacherId,
                "studentCount" to classModel.studentCount,
                "createdAt" to classModel.createdAt
            )
            getClassesRef(accountId, schoolId).document(classModel.id).set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteClass(accountId: String, schoolId: String, classId: String): Result<Unit> {
        return try {
            getClassesRef(accountId, schoolId).document(classId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getClasses(accountId: String, schoolId: String): Result<List<ClassModel>> {
        return try {
            val snapshot = getClassesRef(accountId, schoolId).get().await()
            val classes = snapshot.documents.mapNotNull { doc ->
                try {
                    ClassModel(
                        id = doc.id,
                        schoolId = doc.getString("schoolId") ?: "",
                        name = doc.getString("name") ?: "",
                        grade = doc.getString("grade") ?: "",
                        teacherId = doc.getString("teacherId"),
                        studentCount = doc.getLong("studentCount")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } catch (e: Exception) { null }
            }
            Result.Success(classes)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}