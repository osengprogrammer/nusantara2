package com.azuratech.azuratime.features.biometric.data.local

import androidx.room.*
import com.azuratech.azuratime.features.biometric.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.core.data.local.FaceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface BiometricFaceDao {
    @Upsert
    suspend fun upsertStudentFace(studentFace: BiometricFaceEntity)

    @Upsert
    suspend fun upsertAllStudentFaces(studentFaces: List<BiometricFaceEntity>)

    @Delete
    suspend fun deleteStudentFace(studentFace: BiometricFaceEntity)

    @Query("DELETE FROM faces WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteStudentFaceById(studentId: String, schoolId: String)

    @Query("SELECT * FROM faces WHERE studentId = :studentId AND schoolId = :schoolId LIMIT 1")
    suspend fun getStudentFaceById(studentId: String, schoolId: String): BiometricFaceEntity?

    @Query("SELECT * FROM faces WHERE studentId = :studentId AND schoolId = :schoolId LIMIT 1")
    suspend fun getStudentFaceByIdentity(studentId: String, schoolId: String): BiometricFaceEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM faces WHERE studentId = :studentId AND schoolId = :schoolId)")
    suspend fun isStudentFaceExists(studentId: String, schoolId: String): Boolean

    @Query("SELECT * FROM faces WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    fun getAllStudentsForScanning(schoolId: String): Flow<List<BiometricFaceEntity>>

    @Query("SELECT * FROM faces WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    suspend fun getAllStudentsForScanningList(schoolId: String): List<BiometricFaceEntity>
    
    @Transaction
    @Query("""
        SELECT faces.*, 
               classes.name as className,
               classes.id as classId
        FROM faces
        LEFT JOIN face_assignments ON faces.studentId = face_assignments.studentId AND face_assignments.schoolId = :schoolId
        LEFT JOIN classes ON face_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE faces.schoolId = :schoolId
        ORDER BY faces.name ASC
    """)
    fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>>

    @Transaction
    @Query("""
        SELECT faces.*,
               classes.name as className,
                classes.id as classId
        FROM faces
        LEFT JOIN face_assignments ON faces.studentId = face_assignments.studentId AND face_assignments.schoolId = :schoolId
        LEFT JOIN classes ON face_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE faces.schoolId = :schoolId AND faces.studentId = :studentId
        LIMIT 1
    """)
    suspend fun getStudentWithDetails(studentId: String, schoolId: String): FaceWithDetails?

    @Query("DELETE FROM faces WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("UPDATE faces SET isDeleted = 1, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun markPendingDeletion(studentId: String, schoolId: String)

    // 🔥 Added for DataIntegrityRepository and ReportRepository
    @Query("SELECT COUNT(*) FROM faces WHERE schoolId = :schoolId")
    fun getTotalStudentsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM faces WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedStudentsCountFlow(schoolId: String): Flow<Int>

    @Query("""
        SELECT * FROM faces 
        WHERE schoolId = :schoolId 
        AND studentId NOT IN (SELECT studentId FROM face_assignments WHERE schoolId = :schoolId)
    """)
    fun getStudentsMissingAssignment(schoolId: String): Flow<List<BiometricFaceEntity>>

    @Query("SELECT * FROM faces WHERE schoolId = :schoolId")
    fun getAllStudentsFlow(schoolId: String): Flow<List<BiometricFaceEntity>>

    @Query("DELETE FROM faces WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteStudentFacesByIdentity(studentId: String, schoolId: String)

    @Query("DELETE FROM faces WHERE studentId = :studentId AND schoolId = :schoolId AND studentId != :keepStudentId")
    suspend fun deleteOtherFacesForStudent(studentId: String, keepStudentId: String, schoolId: String)

    @Query("SELECT * FROM faces WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedStudents(schoolId: String): List<BiometricFaceEntity>
}
