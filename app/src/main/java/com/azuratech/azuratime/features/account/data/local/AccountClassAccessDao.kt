package com.azuratech.azuratime.features.account.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountClassAccessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(access: AccountClassAccessEntity)

    @Query("DELETE FROM account_class_access WHERE accountId = :accountId AND schoolId = :schoolId")
    suspend fun deleteByAccount(accountId: String, schoolId: String)

    @Query("DELETE FROM account_class_access WHERE accountId = :accountId AND classId = :classId AND subjectId = :subjectId")
    suspend fun deleteSpecificAssignment(accountId: String, classId: String, subjectId: String)

    @Query("DELETE FROM account_class_access WHERE accountId = :accountId AND classId = :classId")
    suspend fun deleteSpecificAccess(accountId: String, classId: String)

    @Query("SELECT classId, subjectId FROM account_class_access WHERE accountId = :accountId AND schoolId = :schoolId")
    fun getAssignmentsFlow(accountId: String, schoolId: String): Flow<List<TeacherAssignmentTuple>>

    @Query("SELECT EXISTS(SELECT 1 FROM account_class_access WHERE accountId = :accountId AND classId = :classId AND (subjectId = :subjectId OR subjectId = ''))")
    suspend fun hasAccess(accountId: String, classId: String, subjectId: String): Boolean
}

data class TeacherAssignmentTuple(
    val classId: String,
    val subjectId: String,
)
