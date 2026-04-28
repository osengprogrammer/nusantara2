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
    @Query("""
        SELECT c.* FROM classes c
        JOIN school_class_assignments sca ON c.id = sca.classId
        WHERE sca.schoolId = :schoolId
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun observeClassesBySchool(schoolId: String): Flow<List<ClassEntity>>

    /** Used for UI chips or specific selections */
    @Query("""
        SELECT c.* FROM classes c
        JOIN school_class_assignments sca ON c.id = sca.classId
        WHERE sca.schoolId = :schoolId AND c.id IN (:ids)
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
    fun getClassesByIdsFlow(schoolId: String, ids: List<String>): Flow<List<ClassEntity>>

    // =====================================================
    // 📖 READ — ONE-SHOT suspend
    // =====================================================

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): ClassEntity?

    @Query("SELECT * FROM classes WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getClassByName(name: String): ClassEntity?

    @Query("""
        SELECT c.* FROM classes c
        JOIN school_class_assignments sca ON c.id = sca.classId
        WHERE sca.schoolId = :schoolId
        ORDER BY c.displayOrder ASC, c.name ASC
    """)
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

    /** Removes only class assignments belonging to a specific school. */
    @Query("DELETE FROM school_class_assignments WHERE schoolId = :schoolId")
    suspend fun deleteBySchoolId(schoolId: String)

    @Query("DELETE FROM classes")
    suspend fun deleteAll()
}