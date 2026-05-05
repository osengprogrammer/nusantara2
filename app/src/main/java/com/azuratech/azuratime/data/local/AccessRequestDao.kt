package com.azuratech.azuratime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: AccessRequestEntity)

    @Query("SELECT * FROM access_requests WHERE requesterId = :userId ORDER BY updatedAt DESC")
    fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>>

    @Query("SELECT * FROM access_requests WHERE requestId = :requestId")
    suspend fun getRequestById(requestId: String): AccessRequestEntity?

    @Query("SELECT * FROM access_requests WHERE requesterId = :userId AND schoolId = :schoolId LIMIT 1")
    suspend fun getRequestByUserAndSchool(userId: String, schoolId: String): AccessRequestEntity?

    @Query("DELETE FROM access_requests WHERE requestId = :requestId")
    suspend fun deleteRequest(requestId: String)

    @Query("SELECT * FROM access_requests WHERE requesterId = :userId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedRequestsByUser(userId: String): List<AccessRequestEntity>
}
