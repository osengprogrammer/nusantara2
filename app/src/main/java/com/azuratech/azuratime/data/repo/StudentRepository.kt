package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.StudentEntity
import com.azuratech.azuraengine.result.Result

/**
 * Repository interface for Student operations.
 * Enforces Clean Architecture by abstracting data sources (Firestore/Room).
 */
interface StudentRepository {
    suspend fun saveStudent(student: StudentEntity): Result<Unit>
    suspend fun saveStudentToCloud(studentId: String, schoolId: String, data: Map<String, Any>): Result<Unit>
    suspend fun getStudentById(studentId: String, schoolId: String): StudentEntity?
}
