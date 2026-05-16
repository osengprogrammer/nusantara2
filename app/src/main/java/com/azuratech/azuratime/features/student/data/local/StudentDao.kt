package com.azuratech.azuratime.features.student.data.local

import androidx.room.*
import com.azuratech.azuratime.core.data.local.RawStudentProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Upsert
    suspend fun upsertAll(students: List<StudentEntity>)

    @Query("SELECT * FROM students WHERE studentId = :id AND schoolId = :schoolId LIMIT 1")
    suspend fun getById(id: String, schoolId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE schoolId = :schoolId")
    fun getAllFlow(schoolId: String): Flow<List<StudentEntity>>

    @Query("DELETE FROM students WHERE studentId = :id AND schoolId = :schoolId")
    suspend fun deleteById(id: String, schoolId: String)

    @Query("UPDATE students SET classId = :classId, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun updateClassId(studentId: String, schoolId: String, classId: String?)

    @Query("DELETE FROM students WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Transaction
    @Query("""
        SELECT students.*, 
               student_biometrics.studentId as faceId,
               student_biometrics.embedding as embedding,
               student_biometrics.photoUrl as photoUrl,
               student_biometrics.lastUpdated as faceLastUpdated,
               student_biometrics.isSynced as faceIsSynced,
               student_biometrics.isDeleted as faceIsDeleted
        FROM students
        LEFT JOIN student_biometrics ON students.studentId = student_biometrics.studentId AND student_biometrics.schoolId = :schoolId
        WHERE students.schoolId = :schoolId AND students.isDeleted = 0
    """)
    fun getStudentProfilesFlow(schoolId: String): Flow<List<RawStudentProfile>>

    @Query("UPDATE students SET isDeleted = 1, isSynced = 0 WHERE studentId = :studentId AND schoolId = :schoolId")
    suspend fun markPendingDeletion(studentId: String, schoolId: String)

    @Query("SELECT * FROM students WHERE isSynced = 0 AND schoolId = :schoolId")
    suspend fun getUnsyncedStudents(schoolId: String): List<StudentEntity>
}
