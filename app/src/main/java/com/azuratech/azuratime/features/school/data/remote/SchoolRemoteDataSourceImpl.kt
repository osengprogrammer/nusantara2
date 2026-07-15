package com.azuratech.azuratime.features.school.data.remote
import com.azuratech.azuratime.core.data.local.GpsGeofenceEntity
import com.azuratech.azuratime.features.school.domain.model.School
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.result.AppError
import com.azuratech.azuratime.core.result.Result
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
    private val db: FirebaseFirestore,
) : SchoolRemoteDataSource {

    private fun getAccountRef(_accountId: String) = db.collection("accounts").document(_accountId)
    private fun getSchoolsRef(accountId: String) = getAccountRef(accountId).collection("schools")

    // 🔥 Top-level collection for school discovery
    private fun getGlobalSchoolsRef() = db.collection("schools")

    private fun getClassesRef(schoolId: String) =
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
                "updatedAt" to school.updatedAt,
            )
            getGlobalSchoolsRef().document(school.id).set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteSchool(accountId: String, schoolId: String): Result<Unit> {
        return try {
            getGlobalSchoolsRef().document(schoolId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun observeRemoteSchoolsFlow(accountId: String): Flow<Result<List<School>>> = callbackFlow {
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
                            val createdAtRaw = doc.get("createdAt")
                            val createdAt = when (createdAtRaw) {
                                is com.google.firebase.Timestamp -> createdAtRaw.toDate().time
                                is Number -> createdAtRaw.toLong()
                                else -> 0L
                            }
                            val updatedAtRaw = doc.get("updatedAt")
                            val updatedAt = when (updatedAtRaw) {
                                is com.google.firebase.Timestamp -> updatedAtRaw.toDate().time
                                is Number -> updatedAtRaw.toLong()
                                else -> 0L
                            }

                            School(
                                id = doc.id,
                                accountId = doc.getString("accountId") ?: "",
                                name = doc.getString("name") ?: doc.getString("schoolName") ?: "",
                                timezone = doc.getString("timezone") ?: "UTC",
                                status = doc.getString("status") ?: "ACTIVE",
                                createdAt = createdAt,
                                updatedAt = updatedAt,
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
                    val createdAtRaw = doc.get("createdAt")
                    val createdAt = when (createdAtRaw) {
                        is com.google.firebase.Timestamp -> createdAtRaw.toDate().time
                        is Number -> createdAtRaw.toLong()
                        else -> 0L
                    }
                    val updatedAtRaw = doc.get("updatedAt")
                    val updatedAt = when (updatedAtRaw) {
                        is com.google.firebase.Timestamp -> updatedAtRaw.toDate().time
                        is Number -> updatedAtRaw.toLong()
                        else -> 0L
                    }

                    School(
                        id = doc.id,
                        accountId = doc.getString("accountId") ?: "",
                        name = doc.getString("name") ?: doc.getString("schoolName") ?: "",
                        timezone = doc.getString("timezone") ?: "UTC",
                        status = doc.getString("status") ?: "ACTIVE",
                        createdAt = createdAt,
                        updatedAt = updatedAt,
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

            val allSchools = mutableListOf<School>()
            val chunks = schoolIds.chunked(10)

            for (chunk in chunks) {
                // 🔥 Primary: Query by 'schoolId' field
                var snapshot = getGlobalSchoolsRef()
                    .whereIn("schoolId", chunk)
                    .get().await()

                if (snapshot.isEmpty) {
                    // 🔥 Fallback: Try Document ID
                    snapshot = getGlobalSchoolsRef()
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get().await()
                }

                val schools = snapshot.documents.mapNotNull { doc ->
                    try {
                        val createdAtRaw = doc.get("createdAt")
                        val createdAt = when (createdAtRaw) {
                            is com.google.firebase.Timestamp -> createdAtRaw.toDate().time
                            is Number -> createdAtRaw.toLong()
                            else -> 0L
                        }
                        val updatedAtRaw = doc.get("updatedAt")
                        val updatedAt = when (updatedAtRaw) {
                            is com.google.firebase.Timestamp -> updatedAtRaw.toDate().time
                            is Number -> updatedAtRaw.toLong()
                            else -> 0L
                        }

                        School(
                            id = doc.getString("schoolId") ?: doc.id,
                            accountId = doc.getString("accountId") ?: "",
                            name = doc.getString("name") ?: doc.getString("schoolName") ?: "Unknown School",
                            timezone = doc.getString("timezone") ?: "Asia/Jakarta",
                            status = doc.getString("status") ?: "ACTIVE",
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                        )
                    } catch (e: Exception) { null }
                }
                allSchools.addAll(schools)
            }
            Result.Success(allSchools.distinctBy { it.id })
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun saveClass(_accountId: String, schoolId: String, classModel: ClassModel): Result<Unit> {
        return try {
            val data = hashMapOf(
                "id" to classModel.id,
                "schoolId" to classModel.schoolId,
                "name" to classModel.name,
                "grade" to classModel.grade,
                "accountId" to classModel.accountId,
                "studentCount" to classModel.studentCount,
                "createdAt" to classModel.createdAt,
            )
            getClassesRef(schoolId).document(classModel.id).set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteClass(_accountId: String, schoolId: String, classId: String): Result<Unit> {
        return try {
            getClassesRef(schoolId).document(classId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getClasses(_accountId: String, schoolId: String): Result<List<ClassModel>> {
        return try {
            val ref = getClassesRef(schoolId)
            println("🔍 SYNC: Fetching classes from: ${ref.path}")
            val snapshot = ref.get().await()
            println("🔍 SYNC: Found ${snapshot.size()} documents in Firestore classes.")

            val classes = snapshot.documents.mapNotNull { doc ->
                try {
                    val createdAtRaw = doc.get("createdAt")
                    val createdAt = when (createdAtRaw) {
                        is com.google.firebase.Timestamp -> createdAtRaw.toDate().time
                        is Number -> createdAtRaw.toLong()
                        else -> 0L
                    }

                    @Suppress("UNCHECKED_CAST")
                    val studentIds = doc.get("studentIds") as? List<String> ?: emptyList()

                    val classModel = ClassModel(
                        id = doc.id,
                        schoolId = doc.getString("schoolId") ?: schoolId,
                        name = doc.getString("name") ?: "",
                        grade = doc.getString("grade") ?: "",
                        accountId = doc.getString("accountId") ?: "",
                        studentCount = doc.getLong("studentCount")?.toInt() ?: studentIds.size,
                        studentIds = studentIds,
                        createdAt = createdAt,
                    )
                    println("🔍 SYNC: Parsed class: ${classModel.name} (${classModel.id})")
                    classModel
                } catch (e: Exception) {
                    println("❌ SYNC: Failed to parse document ${doc.id}. Error: ${e.message}")
                    println("❌ SYNC: Document Data: ${doc.data}")
                    null
                }
            }
            Result.Success(classes)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun addStudentToClass(schoolId: String, classId: String, studentId: String): Result<Unit> {
        return try {
            getClassesRef(schoolId).document(classId).update(
                "studentIds",
                com.google.firebase.firestore.FieldValue.arrayUnion(studentId),
            ).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    // 📍 GPS GEOFENCE
    override suspend fun saveGeofence(
        schoolId: String,
        geofence: com.azuratech.azuratime.core.data.local.GpsGeofenceEntity,
    ): Result<Unit> {
        return try {
            val data = hashMapOf(
                "id" to geofence.id,
                "schoolId" to geofence.schoolId,
                "latitude" to geofence.latitude,
                "longitude" to geofence.longitude,
                "radiusMeters" to geofence.radiusMeters,
                "isActive" to geofence.isActive,
                "updatedAt" to com.google.firebase.Timestamp.now(),
            )
            db.collection("gps_geofences").document(schoolId).set(data, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun observeGeofenceFlow(schoolId: String): Flow<Result<com.azuratech.azuratime.core.data.local.GpsGeofenceEntity?>> = callbackFlow {
        val subscription = db.collection("gps_geofences").document(schoolId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Failure(AppError.Network(error.message)))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val geofence = com.azuratech.azuratime.core.data.local.GpsGeofenceEntity(
                        id = snapshot.getString("id") ?: snapshot.id,
                        schoolId = snapshot.getString("schoolId") ?: schoolId,
                        latitude = snapshot.getDouble("latitude") ?: 0.0,
                        longitude = snapshot.getDouble("longitude") ?: 0.0,
                        radiusMeters = snapshot.getLong("radiusMeters")?.toInt() ?: 100,
                        isActive = snapshot.getBoolean("isActive") ?: false,
                        syncStatus = "SYNCED",
                    )
                    trySend(Result.Success(geofence))
                } else {
                    trySend(Result.Success(null))
                }
            }
        awaitClose { subscription.remove() }
    }
}
