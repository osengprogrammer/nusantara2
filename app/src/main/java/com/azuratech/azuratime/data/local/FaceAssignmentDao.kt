package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceAssignmentDao {
    @Upsert
    suspend fun upsert(assignment: FaceAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity)

    @Query("DELETE FROM face_assignments WHERE faceId = :faceId AND classId = :classId AND schoolId = :schoolId")
    suspend fun deleteSpecificAssignment(faceId: String, classId: String, schoolId: String)

    @Query("DELETE FROM face_assignments WHERE faceId = :faceId AND schoolId = :schoolId")
    suspend fun deleteAllByFace(faceId: String, schoolId: String)

    @Query("DELETE FROM face_assignments WHERE faceId = :faceId AND schoolId = :schoolId")
    suspend fun deleteAssignmentsForFace(faceId: String, schoolId: String)

    @Query("UPDATE face_assignments SET classId = :newClassId, isSynced = 0 WHERE faceId = :faceId AND schoolId = :schoolId")
    suspend fun updateClassForFace(faceId: String, newClassId: String, schoolId: String)

    @Query("DELETE FROM face_assignments WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("SELECT classId FROM face_assignments WHERE faceId = :faceId AND schoolId = :schoolId")
    fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>>

    @Query("SELECT * FROM face_assignments WHERE schoolId = :schoolId")
    fun getAllAssignments(schoolId: String): Flow<List<FaceAssignmentEntity>>

    @Query("SELECT faces.* FROM faces INNER JOIN face_assignments ON faces.faceId = face_assignments.faceId WHERE face_assignments.classId = :classId AND faces.schoolId = :schoolId")
    fun getFacesByClass(classId: String, schoolId: String): Flow<List<FaceEntity>>

    @Query("SELECT COUNT(DISTINCT faceId) FROM face_assignments WHERE classId = :classId AND schoolId = :schoolId")
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>

    @Query("UPDATE face_assignments SET isSynced = :status WHERE faceId = :faceId AND classId = :classId AND schoolId = :schoolId")
    suspend fun updateSyncStatus(faceId: String, classId: String, schoolId: String, status: Boolean)

    @Query("SELECT COUNT(*) FROM faces WHERE schoolId = :schoolId AND faceId NOT IN (SELECT faceId FROM face_assignments WHERE schoolId = :schoolId)")
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM face_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedAssignmentsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT * FROM face_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedAssignments(schoolId: String): List<FaceAssignmentEntity>

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId")
    suspend fun getAssignmentCount(schoolId: String): Int

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId AND classId NOT IN (SELECT id FROM classes WHERE schoolId = :schoolId)")
    fun getBrokenAssignmentsCount(schoolId: String): Flow<Int>

    // 🔥 Added for ReportRepository
    @Query("SELECT faces.* FROM faces INNER JOIN face_assignments ON faces.faceId = face_assignments.faceId WHERE face_assignments.classId IN (:classIds) AND faces.schoolId = :schoolId")
    fun getFacesByMultipleClasses(classIds: List<String>, schoolId: String): Flow<List<FaceEntity>>
}
