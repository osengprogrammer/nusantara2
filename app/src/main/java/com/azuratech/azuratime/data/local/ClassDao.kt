package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 🏫 CLASS DAO
 * Dedicated DAO for managing school classes.
 * Replaces the old complex OptionDao.
 */
@Dao
interface ClassDao {

    // =====================================================
    // 📖 READ — FLOWS (Reactive UI)
    // =====================================================

    /** School-scoped reactive query — switches automatically when activeSchoolId changes. */
    @Query("SELECT * FROM classes WHERE schoolId = :schoolId ORDER BY displayOrder ASC, name ASC")
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>

    /** Used for UI chips or specific selections */
    @Query("SELECT * FROM classes WHERE schoolId = :schoolId AND id IN (:ids) ORDER BY displayOrder ASC, name ASC")
    fun getClassesByIdsFlow(schoolId: String, ids: List<String>): Flow<List<ClassEntity>>

    // =====================================================
    // 📖 READ — ONE-SHOT suspend
    // =====================================================

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): ClassEntity?

    @Query("SELECT * FROM classes WHERE schoolId = :schoolId AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getClassByName(schoolId: String, name: String): ClassEntity?

    @Query("SELECT * FROM classes WHERE schoolId = :schoolId ORDER BY displayOrder ASC, name ASC")
    suspend fun getClassesBySchoolOnce(schoolId: String): List<ClassEntity>

    /** Safety guard: count faces assigned to a class before allowing delete. */
    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun getStudentCountForClass(schoolId: String, classId: String): Int

    // =====================================================
    // ☁️ CLOUD SYNC
    // =====================================================

    @Query("SELECT * FROM classes WHERE isSynced = 0")
    suspend fun getUnsyncedClasses(): List<ClassEntity>

    @Query("UPDATE classes SET isSynced = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: Boolean)

    // =====================================================
    // ✏️ WRITE
    // =====================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(classEntity: ClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classes: List<ClassEntity>)

    @Update
    suspend fun update(classEntity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Removes only classes belonging to a specific school. */
    @Query("DELETE FROM classes WHERE schoolId = :schoolId")
    suspend fun deleteBySchoolId(schoolId: String)

    @Query("DELETE FROM classes")
    suspend fun deleteAll()
}