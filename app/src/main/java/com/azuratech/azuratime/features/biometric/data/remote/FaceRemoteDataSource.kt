package com.azuratech.azuratime.features.biometric.data.remote

import com.azuratech.azuratime.features.biometric.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuraengine.result.Result

interface FaceRemoteDataSource {
    suspend fun getFaceUpdates(schoolId: String, lastSync: Long): Result<List<Pair<BiometricFaceEntity, Boolean>>>
    suspend fun uploadFacePhoto(schoolId: String, studentId: String, imageBytes: ByteArray): Result<String?>
    suspend fun bulkSyncFaces(schoolId: String, students: List<BiometricFaceEntity>): Result<Unit>
    suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity): Result<Unit>
    suspend fun deleteStudent(studentId: String, schoolId: String, classIds: List<String>): Result<Unit>
}
