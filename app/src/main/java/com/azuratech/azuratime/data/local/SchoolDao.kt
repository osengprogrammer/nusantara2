package com.azuratech.azuratime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: SchoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schools: List<SchoolEntity>)

    @Query("SELECT * FROM schools WHERE id = :schoolId LIMIT 1")
    suspend fun getSchoolById(schoolId: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE id = :schoolId LIMIT 1")
    fun observeSchoolById(schoolId: String): Flow<SchoolEntity?>

    // Useful if a user is an admin of multiple schools
    @Query("SELECT * FROM schools ORDER BY name ASC")
    fun observeAllSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchSchools(query: String): List<SchoolEntity>

    @Query("DELETE FROM schools WHERE id = :schoolId")
    suspend fun deleteSchool(schoolId: String)
}