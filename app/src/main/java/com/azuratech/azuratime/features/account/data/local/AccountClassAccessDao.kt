package com.azuratech.azuratime.features.account.data.local
import com.azuratech.azuratime.core.domain.model.TeacherAssignment

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

    @Query("SELECT classId, subjectId FROM account_class_access WHERE accountId = :accountId AND schoolId = :schoolId AND isActive = 1")
    fun getAssignmentsFlow(accountId: String, schoolId: String): Flow<List<TeacherAssignmentTuple>>

    @Query("SELECT EXISTS(SELECT 1 FROM account_class_access WHERE accountId = :accountId AND classId = :classId AND (subjectId = :subjectId OR subjectId = '') AND isActive = 1)")
    suspend fun hasAccess(accountId: String, classId: String, subjectId: String): Boolean

    /**
     * ✅ FUNGSI ATOMIC: Mengganti penugasan lama dengan yang baru.
     * Menggunakan pendekatan "Soft-Delete & Insert" agar tidak ada
     * gap waktu di mana supervisor tidak memiliki akses.
     */
    @Transaction
    suspend fun replaceAssignments(
        accountId: String,
        schoolId: String,
        newAssignments: List<AccountClassAccessEntity>,
    ) {
        // 1. Nonaktifkan penugasan lama secara halus (Soft Delete)
        deactivateOldAssignments(accountId, schoolId)

        // 2. Masukkan penugasan baru dari template
        insertAll(newAssignments)
    }

    @Query(
        """
        UPDATE account_class_access 
        SET isActive = 0 
        WHERE accountId = :accountId 
          AND schoolId = :schoolId 
          AND isActive = 1
    """,
    )
    suspend fun deactivateOldAssignments(accountId: String, schoolId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<AccountClassAccessEntity>)

    /**
     * 🔑 AUTHORIZATION HEART
     * Mengambil matrix lengkap (Kelas + Mapel) untuk Supervisor di Workspace tertentu.
     * Menggunakan filter isActive untuk mendukung audit trail (Soft Delete).
     */
    @Transaction
    @Query(
        """
        SELECT 
            aca.classId, 
            c.name as className, 
            aca.subjectId, 
            COALESCE(s.name, 'Semua Mapel / Wali Kelas') as subjectName
        FROM account_class_access aca
        INNER JOIN classes c ON aca.classId = c.id
        LEFT JOIN subjects s ON aca.subjectId = s.subjectId
        WHERE aca.accountId = :accountId 
          AND aca.schoolId = :schoolId
          AND aca.isActive = 1
        ORDER BY c.name ASC
    """,
    )
    fun getSupervisorMatrixFlow(accountId: String, schoolId: String): Flow<List<SupervisorMatrixTuple>>
}

data class TeacherAssignmentTuple(
    val classId: String,
    val subjectId: String,
)

data class SupervisorMatrixTuple(
    val classId: String,
    val className: String,
    val subjectId: String,
    val subjectName: String,
)
