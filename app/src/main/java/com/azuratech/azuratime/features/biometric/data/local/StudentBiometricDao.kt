package com.azuratech.azuratime.features.biometric.data.local

import com.azuratech.azuratime.core.data.local.StudentBiometricDetails
import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentBiometricDao {
    @Upsert
    suspend fun upsertStudentBiometric(biometric: StudentBiometricEntity)

    @Upsert
    suspend fun upsertAllStudentBiometrics(biometrics: List<StudentBiometricEntity>)

    @Delete
    suspend fun deleteStudentBiometric(biometric: StudentBiometricEntity)

    @Query("DELETE FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteStudentBiometricById(studentId: String, schoolId: String)

    @Query("SELECT * FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId LIMIT 1")
    suspend fun getStudentBiometricById(studentId: String, schoolId: String): StudentBiometricEntity?

    @Query("SELECT * FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId LIMIT 1")
    suspend fun getStudentBiometricByIdentity(studentId: String, schoolId: String): StudentBiometricEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId)")
    suspend fun isStudentBiometricExists(studentId: String, schoolId: String): Boolean

    @Query("SELECT * FROM student_biometrics WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    fun getAllStudentsForScanning(schoolId: String): Flow<List<StudentBiometricEntity>>

    @Query("SELECT * FROM student_biometrics WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    suspend fun getAllStudentsForScanningList(schoolId: String): List<StudentBiometricEntity>
    
    @Transaction
    @Query("""
        SELECT student_biometrics.*, 
               classes.name as className,
               classes.id as classId
        FROM student_biometrics
        LEFT JOIN student_class_assignments ON student_biometrics.studentId = student_class_assignments.studentId AND student_class_assignments.schoolId = :schoolId
        LEFT JOIN classes ON student_class_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE student_biometrics.schoolId = :schoolId
        ORDER BY student_biometrics.name ASC
    """)
    fun getAllStudentsWithDetailsFlow(schoolId: String): Flow<List<StudentBiometricDetails>>

    @Transaction
    @Query("""
        SELECT student_biometrics.*,
               classes.name as className,
                classes.id as classId
        FROM student_biometrics
        LEFT JOIN student_class_assignments ON student_biometrics.studentId = student_class_assignments.studentId AND student_class_assignments.schoolId = :schoolId
        LEFT JOIN classes ON student_class_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE student_biometrics.schoolId = :schoolId AND student_biometrics.studentId = :studentId
        LIMIT 1
    """)
    suspend fun getStudentWithDetails(studentId: String, schoolId: String): StudentBiometricDetails?

    @Query("DELETE FROM student_biometrics WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("UPDATE student_biometrics SET isDeleted = 1, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun markPendingDeletion(studentId: String, schoolId: String)

    // 🔥 Added for DataIntegrityRepository and ReportRepository
    @Query("SELECT COUNT(*) FROM student_biometrics WHERE schoolId = :schoolId")
    fun getTotalStudentsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM student_biometrics WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedStudentsCountFlow(schoolId: String): Flow<Int>

    @Query("""
        SELECT * FROM student_biometrics 
        WHERE schoolId = :schoolId 
        AND studentId NOT IN (SELECT studentId FROM student_class_assignments WHERE schoolId = :schoolId)
    """)
    fun getStudentsMissingAssignment(schoolId: String): Flow<List<StudentBiometricEntity>>

    @Query("SELECT * FROM student_biometrics WHERE schoolId = :schoolId")
    fun getAllStudentsFlow(schoolId: String): Flow<List<StudentBiometricEntity>>

    @Query("DELETE FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteStudentBiometricsByIdentity(studentId: String, schoolId: String)

    @Query("DELETE FROM student_biometrics WHERE studentId = :studentId AND schoolId = :schoolId AND studentId != :keepStudentId")
    suspend fun deleteOtherBiometricsForStudent(studentId: String, keepStudentId: String, schoolId: String)

    @Query("SELECT * FROM student_biometrics WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedBiometrics(schoolId: String): List<StudentBiometricEntity>
}
