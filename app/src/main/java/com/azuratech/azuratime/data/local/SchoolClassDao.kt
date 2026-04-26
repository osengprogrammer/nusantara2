package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM schools WHERE accountId = :accountId ORDER BY name ASC")
    fun getSchools(accountId: String): Flow<List<SchoolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchool(school: SchoolEntity)

    @Query("SELECT * FROM classes WHERE schoolId = :schoolId ORDER BY grade, name ASC")
    fun getClasses(schoolId: String): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClass(classEntity: ClassEntity)

    @Query("DELETE FROM schools WHERE id = :id")
    suspend fun deleteSchoolById(id: String)

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun getStudentCountForClass(schoolId: String, classId: String): Int

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    @Query("""
        SELECT classes.* FROM classes 
        INNER JOIN schools ON classes.schoolId = schools.id 
        WHERE schools.accountId = :accountId 
        ORDER BY schools.name, classes.name ASC
    """)
    fun getAllClassesForAccount(accountId: String): Flow<List<ClassEntity>>

    @Query("UPDATE classes SET schoolId = :newSchoolId WHERE id = :classId")
    suspend fun reassignClass(classId: String, newSchoolId: String)
}
