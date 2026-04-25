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
}
