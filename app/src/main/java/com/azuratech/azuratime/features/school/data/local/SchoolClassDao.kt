package com.azuratech.azuratime.features.school.data.local

import androidx.room.*
import com.azuratech.azuratime.core.data.local.ClassEntity
import com.azuratech.azuratime.core.data.local.GpsGeofenceEntity
import com.azuratech.azuratime.core.data.local.SchoolEntity
import com.azuratech.azuratime.core.data.local.SchoolClassAssignment
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM schools WHERE accountId = :accountId AND status != 'DELETED' ORDER BY name ASC")
    fun getSchoolsFlow(accountId: String): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE id IN (:schoolIds) AND status != 'DELETED' ORDER BY name ASC")
    fun observeSchoolsByIdsFlow(schoolIds: List<String>): Flow<List<SchoolEntity>>

    @Query("SELECT * FROM schools WHERE status != 'DELETED'")
    suspend fun getAllSchoolsOnce(): List<SchoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchool(school: SchoolEntity)

    @Query("SELECT * FROM schools WHERE id = :id")
    suspend fun getSchoolById(id: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE id = :id")
    fun observeSchoolByIdFlow(id: String): Flow<SchoolEntity?>

    @Query("SELECT * FROM schools WHERE status != 'DELETED' ORDER BY createdAt DESC")
    fun observeAllSchoolsFlow(): Flow<List<SchoolEntity>>

    @Query("SELECT id FROM schools WHERE accountId = :accountId LIMIT 1")
    suspend fun getFirstSchoolId(accountId: String): String?

    @Query("SELECT COUNT(*) FROM schools WHERE accountId = :accountId")
    suspend fun getSchoolCountByAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM classes WHERE accountId = :accountId")
    suspend fun getClassCountByAccount(accountId: String): Int

    @Query(
        """
        SELECT * FROM classes
        WHERE schoolId = :schoolId
        OR id IN (SELECT classId FROM school_class_assignments WHERE schoolId = :schoolId)
        ORDER BY grade, name ASC
    """,
    )
    fun getClassesFlow(schoolId: String): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignClass(assignment: SchoolClassAssignment)

    @Query("DELETE FROM school_class_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun unassignClass(schoolId: String, classId: String)

    @Query("SELECT classId FROM school_class_assignments WHERE schoolId = :schoolId")
    suspend fun getAssignedClassIds(schoolId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertClass(classEntity: ClassEntity)

    @Query(
        """
        SELECT * FROM classes
        WHERE (schoolId = :schoolId OR id IN (SELECT classId FROM school_class_assignments WHERE schoolId = :schoolId))
          AND name = :name COLLATE NOCASE
        LIMIT 1
    """,
    )
    suspend fun getClassByNameAndSchool(schoolId: String, name: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClassesIgnore(classes: List<ClassEntity>)

    @Query("DELETE FROM schools WHERE id = :id")
    suspend fun deleteSchoolById(id: String)

    @Query("SELECT COUNT(*) FROM student_class_assignments WHERE schoolId = :schoolId AND classId = :classId")
    suspend fun getStudentCountForClass(schoolId: String, classId: String): Int

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    @Transaction
    suspend fun deleteClassWithAssignments(schoolId: String, classId: String) {
        unassignClass(schoolId, classId)
        deleteClassById(classId)
    }

    @Query("SELECT * FROM classes WHERE accountId = :accountId ORDER BY name ASC")
    fun getAllClassesFlow(accountId: String): Flow<List<ClassEntity>>

    @Query(
        """
        SELECT classes.* FROM classes
        INNER JOIN school_class_assignments ON classes.id = school_class_assignments.classId
        INNER JOIN schools ON school_class_assignments.schoolId = schools.id
        WHERE schools.accountId = :accountId
        ORDER BY schools.name, classes.name ASC
    """,
    )
    fun getAllClassesForAccountFlow(accountId: String): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE schoolId IS NULL OR schoolId = ''")
    suspend fun getOrphanedClasses(): List<ClassEntity>

    @Query("UPDATE classes SET schoolId = :schoolId WHERE id = :classId")
    suspend fun updateClassSchool(classId: String, schoolId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun reassignClass(assignment: SchoolClassAssignment)

    // 📍 GPS GEOFENCE OPERATIONS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGeofence(geofence: GpsGeofenceEntity)

    @Query("SELECT * FROM gps_geofences WHERE schoolId = :schoolId LIMIT 1")
    fun observeGeofenceFlow(schoolId: String): Flow<GpsGeofenceEntity?>

    @Query("DELETE FROM gps_geofences WHERE id = :id")
    suspend fun deleteGeofence(id: String)
}
