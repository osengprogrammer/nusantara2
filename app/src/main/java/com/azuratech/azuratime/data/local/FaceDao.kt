package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {
    @Upsert
    suspend fun upsertFace(face: FaceEntity)

    @Upsert
    suspend fun upsertAll(faces: List<FaceEntity>)

    @Delete
    suspend fun deleteFace(face: FaceEntity)

    @Query("DELETE FROM faces WHERE faceId = :faceId AND schoolId = :schoolId")
    suspend fun deleteFaceById(faceId: String, schoolId: String)

    @Query("SELECT * FROM faces WHERE faceId = :id AND schoolId = :schoolId LIMIT 1")
    suspend fun getFaceById(id: String, schoolId: String): FaceEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM faces WHERE faceId = :id AND schoolId = :schoolId)")
    suspend fun isFaceExists(id: String, schoolId: String): Boolean

    @Query("SELECT * FROM faces WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    fun getAllFacesForScanning(schoolId: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE embedding IS NOT NULL AND schoolId = :schoolId")
    suspend fun getAllFacesForScanningList(schoolId: String): List<FaceEntity>
    
    @Transaction
    @Query("""
        SELECT faces.*, 
               classes.name as className,
               classes.id as classId
        FROM faces
        LEFT JOIN face_assignments ON faces.faceId = face_assignments.faceId AND face_assignments.schoolId = :schoolId
        LEFT JOIN classes ON face_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE faces.schoolId = :schoolId
        ORDER BY faces.name ASC
    """)
    fun getAllFacesWithDetailsFlow(schoolId: String): Flow<List<FaceWithDetails>>

    @Transaction
    @Query("""
        SELECT faces.*,
               classes.name as className,
                classes.id as classId
        FROM faces
        LEFT JOIN face_assignments ON faces.faceId = face_assignments.faceId AND face_assignments.schoolId = :schoolId
        LEFT JOIN classes ON face_assignments.classId = classes.id AND classes.schoolId = :schoolId
        WHERE faces.schoolId = :schoolId AND faces.faceId = :faceId
        LIMIT 1
    """)
    suspend fun getFaceWithDetails(faceId: String, schoolId: String): FaceWithDetails?

    @Query("DELETE FROM faces WHERE schoolId = :schoolId")
    suspend fun deleteAllBySchool(schoolId: String)

    @Query("UPDATE faces SET isDeleted = 1, isSynced = 0 WHERE faceId = :faceId AND schoolId = :schoolId")
    suspend fun markPendingDeletion(faceId: String, schoolId: String)

    // 🔥 Added for DataIntegrityRepository and ReportRepository
    @Query("SELECT COUNT(*) FROM faces WHERE schoolId = :schoolId")
    fun getTotalFacesFlow(schoolId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM faces WHERE isSynced = 0 AND schoolId = :schoolId")
    fun getUnsyncedFacesCountFlow(schoolId: String): Flow<Int>

    @Query("""
        SELECT * FROM faces 
        WHERE schoolId = :schoolId 
        AND faceId NOT IN (SELECT faceId FROM face_assignments WHERE schoolId = :schoolId)
    """)
    fun getFacesMissingAssignment(schoolId: String): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE schoolId = :schoolId")
    fun getAllFacesFlow(schoolId: String): Flow<List<FaceEntity>>
}
