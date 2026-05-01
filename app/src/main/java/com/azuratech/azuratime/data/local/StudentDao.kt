package com.azuratech.azuratime.data.local

import androidx.room.*
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
}
