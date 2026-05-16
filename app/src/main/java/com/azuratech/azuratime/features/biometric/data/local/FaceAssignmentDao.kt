package com.azuratech.azuratime.features.biometric.data.local

import androidx.room.*
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: FaceAssignmentEntity)

    @Query("DELETE FROM face_assignments WHERE studentId = :studentId AND classId = :classId AND schoolId = :schoolId")
    suspend fun deleteSpecificAssignment(studentId: String, classId: String, schoolId: String)

    @Query("DELETE FROM face_assignments WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteAllByStudent(studentId: String, schoolId: String)

    @Query("DELETE FROM face_assignments WHERE studentId = :studentId")
    suspend fun deleteAllByStudentId(studentId: String)

    @Query("DELETE FROM face_assignments WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("SELECT classId FROM face_assignments WHERE studentId = :studentId AND schoolId = :schoolId")
    fun getClassIdsForStudent(studentId: String, schoolId: String): Flow<List<String>>

    @Query("SELECT * FROM face_assignments WHERE schoolId = :schoolId")
    fun getAllAssignments(schoolId: String): Flow<List<FaceAssignmentEntity>>

    @Query("SELECT faces.* FROM faces INNER JOIN face_assignments ON faces.studentId = face_assignments.studentId WHERE face_assignments.classId = :classId AND faces.schoolId = :schoolId")
    fun getStudentsByClass(classId: String, schoolId: String): Flow<List<BiometricFaceEntity>>

    @Query("SELECT COUNT(DISTINCT studentId) FROM face_assignments WHERE classId = :classId AND schoolId = :schoolId")
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>

    @Query("UPDATE face_assignments SET isSynced = :status WHERE studentId = :studentId AND classId = :classId AND schoolId = :schoolId")
    suspend fun updateSyncStatus(studentId: String, classId: String, schoolId: String, status: Boolean)

    @Query("SELECT COUNT(*) FROM faces WHERE schoolId = :schoolId AND studentId NOT IN (SELECT studentId FROM face_assignments WHERE schoolId = :schoolId)")
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM face_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedAssignmentsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT * FROM face_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedAssignments(schoolId: String): List<FaceAssignmentEntity>

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId")
    suspend fun getAssignmentCount(schoolId: String): Int

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId AND classId NOT IN (SELECT id FROM classes WHERE schoolId = :schoolId)")
    fun getBrokenAssignmentsCount(schoolId: String): Flow<Int>

    @Query("UPDATE face_assignments SET classId = :newClassId, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun updateClassForStudent(studentId: String, newClassId: String, schoolId: String)

    // 🔥 Added for ReportRepository
    @Query("SELECT faces.* FROM faces INNER JOIN face_assignments ON faces.studentId = face_assignments.studentId WHERE face_assignments.classId IN (:classIds) AND faces.schoolId = :schoolId")
    fun getStudentsByMultipleClasses(classIds: List<String>, schoolId: String): Flow<List<BiometricFaceEntity>>
}
