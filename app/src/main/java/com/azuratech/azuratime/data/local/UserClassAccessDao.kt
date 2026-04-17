package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserClassAccessDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccess(access: UserClassAccessEntity)

    // 🔥 Get all class IDs a teacher is allowed to scan/view in the current school
    @Query("SELECT classId FROM user_class_access WHERE userId = :userId AND schoolId = :schoolId")
    fun observeClassIdsForUser(userId: String, schoolId: String): Flow<List<String>>

    @Query("SELECT classId FROM user_class_access WHERE userId = :userId AND schoolId = :schoolId")
    suspend fun getClassIdsForUser(userId: String, schoolId: String): List<String>

    // 🔥 Secure Wipe: Only removes access for the school the user is currently syncing
    @Query("DELETE FROM user_class_access WHERE userId = :userId AND schoolId = :schoolId")
    suspend fun clearAllAccessForUserInSchool(userId: String, schoolId: String)

    @Query("DELETE FROM user_class_access WHERE userId = :userId AND classId = :classId AND schoolId = :schoolId")
    suspend fun removeAccess(userId: String, classId: String, schoolId: String)
}