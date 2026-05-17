package com.azuratech.azuratime.core.data.local

import kotlinx.coroutines.flow.Flow

@Dao
interface AccountClassAccessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(access: AccountClassAccessEntity)

    @Query("DELETE FROM account_class_access WHERE accountId = :accountId AND schoolId = :schoolId")
    suspend fun deleteByAccount(accountId: String, schoolId: String)

    @Query("SELECT classId FROM account_class_access WHERE accountId = :accountId AND schoolId = :schoolId")
    fun getAssignedClassIds(accountId: String, schoolId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM account_class_access WHERE accountId = :accountId AND classId = :classId)")
    suspend fun hasAccess(accountId: String, classId: String): Boolean
}
