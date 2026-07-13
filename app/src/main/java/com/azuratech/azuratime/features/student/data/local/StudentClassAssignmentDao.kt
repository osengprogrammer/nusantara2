package com.azuratech.azuratime.features.student.data.local
import com.azuratech.azuratime.core.data.local.StudentClassAssignmentEntity

import androidx.room.*
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentClassAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignment(assignment: StudentClassAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAssignments(assignments: List<StudentClassAssignmentEntity>)

    @Query("DELETE FROM student_class_assignments WHERE studentId = :studentId AND classId = :classId AND schoolId = :schoolId")
    suspend fun deleteSpecificAssignment(studentId: String, classId: String, schoolId: String)

    @Query("DELETE FROM student_class_assignments WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun deleteAllByStudent(studentId: String, schoolId: String)

    @Query("DELETE FROM student_class_assignments WHERE studentId = :studentId")
    suspend fun deleteAllByStudentId(studentId: String)

    @Query("DELETE FROM student_class_assignments WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query(
        """
        SELECT classId FROM student_class_assignments WHERE studentId = :studentId AND schoolId = :schoolId
        UNION
        SELECT classId FROM students WHERE studentId = :studentId AND schoolId = :schoolId AND classId IS NOT NULL
    """,
    )
    fun getClassIdsForStudent(studentId: String, schoolId: String): Flow<List<String>>

    @Query("SELECT * FROM student_class_assignments WHERE schoolId = :schoolId")
    fun getAllAssignments(schoolId: String): Flow<List<StudentClassAssignmentEntity>>

    @Query("SELECT student_biometrics.* FROM student_biometrics INNER JOIN student_class_assignments ON student_biometrics.studentId = student_class_assignments.studentId WHERE student_class_assignments.classId = :classId AND student_biometrics.schoolId = :schoolId")
    fun getStudentsByClass(classId: String, schoolId: String): Flow<List<StudentBiometricEntity>>

    @Query("SELECT COUNT(DISTINCT studentId) FROM student_class_assignments WHERE classId = :classId AND schoolId = :schoolId")
    fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int>

    @Query("UPDATE student_class_assignments SET isSynced = :status WHERE studentId = :studentId AND classId = :classId AND schoolId = :schoolId")
    suspend fun updateSyncStatus(studentId: String, classId: String, schoolId: String, status: Boolean)

    @Query("SELECT COUNT(*) FROM student_biometrics WHERE schoolId = :schoolId AND studentId NOT IN (SELECT studentId FROM student_class_assignments WHERE schoolId = :schoolId)")
    fun getUnassignedStudentCount(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM student_class_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedAssignmentsCountFlow(schoolId: String): Flow<Int>

    @Query("SELECT * FROM student_class_assignments WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedAssignments(schoolId: String): List<StudentClassAssignmentEntity>

    @Query("SELECT COUNT(*) FROM student_class_assignments WHERE schoolId = :schoolId")
    suspend fun getAssignmentCount(schoolId: String): Int

    @Query("SELECT COUNT(*) FROM student_class_assignments WHERE schoolId = :schoolId AND classId NOT IN (SELECT id FROM classes WHERE schoolId = :schoolId)")
    fun getBrokenAssignmentsCount(schoolId: String): Flow<Int>

    @Query("UPDATE student_class_assignments SET classId = :newClassId, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun updateClassForStudent(studentId: String, newClassId: String, schoolId: String)

    // 🔥 Added for ReportRepository
    @Query("SELECT student_biometrics.* FROM student_biometrics INNER JOIN student_class_assignments ON student_biometrics.studentId = student_class_assignments.studentId WHERE student_class_assignments.classId IN (:classIds) AND student_biometrics.schoolId = :schoolId")
    fun getStudentsByMultipleClasses(classIds: List<String>, schoolId: String): Flow<List<StudentBiometricEntity>>
}
