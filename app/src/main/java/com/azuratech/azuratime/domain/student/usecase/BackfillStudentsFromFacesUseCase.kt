package com.azuratech.azuratime.domain.student.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🛠️ BACKFILL STUDENTS FROM FACES
 * 
 * Ensures every record in 'faces' has a corresponding 'students' identity.
 * Necessary for migration to the Student-first architecture (Phase 7.6+).
 */
class BackfillStudentsFromFacesUseCase @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val faceDao = database.faceDao()
    private val studentDao = database.studentDao()

    suspend fun execute() = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext
        
        // 1. Get all faces for this school
        val faces = faceDao.getAllFacesForScanningList(schoolId)
        if (faces.isEmpty()) return@withContext

        println("🔧 Backfill: Checking ${faces.size} faces for missing student identities...")

        var backfilledCount = 0
        faces.forEach { face ->
            val studentId = face.studentId ?: face.faceId
            
            // Check if student exists
            val existing = studentDao.getById(studentId, schoolId)
            if (existing == null) {
                val newStudent = StudentEntity(
                    studentId = studentId,
                    schoolId = schoolId,
                    name = face.name,
                    studentCode = null,
                    classId = null, // Will be linked via face_assignments mapping in StudentProfile
                    createdAt = face.createdAt,
                    isSynced = face.isSynced,
                    isDeleted = face.isDeleted
                )
                studentDao.upsert(newStudent)
                
                // If studentId was null in face, update it
                if (face.studentId == null) {
                    faceDao.upsertFace(face.copy(studentId = studentId))
                }
                
                backfilledCount++
            }
        }

        if (backfilledCount > 0) {
            println("✅ Backfill: Created $backfilledCount student records from legacy face data.")
        }
    }
}
