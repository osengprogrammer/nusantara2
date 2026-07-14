package com.azuratech.azuratime.core.data.local

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
    fun observeSchoolByIdFlow(schoolId: String): Flow<SchoolEntity?>

    @Query("SELECT * FROM schools ORDER BY name ASC")
    fun observeAllSchoolsFlow(): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchSchools(query: String): List<SchoolEntity>

    @Query("DELETE FROM schools WHERE id = :schoolId")
    suspend fun deleteSchool(schoolId: String)
}
