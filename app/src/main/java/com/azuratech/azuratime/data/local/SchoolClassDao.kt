package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM schools WHERE accountId = :accountId ORDER BY name ASC")
    fun getSchools(accountId: String): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools")
    suspend fun getAllSchoolsOnce(): List<SchoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchool(school: SchoolEntity)

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getSchoolById(id: String): SchoolEntity?

    @Query("SELECT * FROM schools ORDER BY createdAt DESC")
    fun observeAllSchools(): Flow<List<SchoolEntity>>

    @Query("SELECT id FROM schools WHERE accountId = :accountId LIMIT 1")
    suspend fun getFirstSchoolId(accountId: String): String?

    @Query("SELECT COUNT(*) FROM schools WHERE accountId = :accountId")
    suspend fun getSchoolCountByAccount(accountId: String): Int

    @Query("""
        SELECT * FROM classes 
        WHERE schoolId = :schoolId 
        OR id IN (SELECT classId FROM school_class_assignments WHERE schoolId = :schoolId)
        ORDER BY grade, name ASC
    """)
    fun getClasses(schoolId: String): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignClass(assignment: SchoolClassAssignment)

    @Query("DELETE FROM school_class_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun unassignClass(schoolId: String, classId: String)

    @Query("SELECT classId FROM school_class_assignments WHERE schoolId = :schoolId")
    suspend fun getAssignedClassIds(schoolId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClass(classEntity: ClassEntity)

    @Query("DELETE FROM schools WHERE id = :id")
    suspend fun deleteSchoolById(id: String)

    @Query("SELECT COUNT(*) FROM face_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun getStudentCountForClass(schoolId: String, classId: String): Int

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    @Query("SELECT * FROM classes WHERE accountId = :accountId ORDER BY name ASC")
    fun getAllClasses(accountId: String): Flow<List<ClassEntity>>

    @Query("""
        SELECT classes.* FROM classes 
        INNER JOIN school_class_assignments ON classes.id = school_class_assignments.classId
        INNER JOIN schools ON school_class_assignments.schoolId = schools.id 
        WHERE schools.accountId = :accountId 
        ORDER BY schools.name, classes.name ASC
    """)
    fun getAllClassesForAccount(accountId: String): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE schoolId IS NULL OR schoolId = ''")
    suspend fun getOrphanedClasses(): List<ClassEntity>

    @Query("UPDATE classes SET schoolId = :schoolId WHERE id = :classId")
    suspend fun updateClassSchool(classId: String, schoolId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun reassignClass(assignment: SchoolClassAssignment)
}
