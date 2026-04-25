package com.azuratech.azuratime.data.remote

import com.azuratech.azuratime.data.local.FaceAssignmentEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuraengine.result.Result

interface FaceRemoteDataSource {
    suspend fun getFaceUpdates(schoolId: String, lastSync: Long): Result<List<Pair<FaceEntity, Boolean>>>
    suspend fun uploadFacePhoto(schoolId: String, faceId: String, imageBytes: ByteArray): Result<String?>
    suspend fun bulkSyncFaces(schoolId: String, faces: List<FaceEntity>): Result<Unit>
    suspend fun syncFaceAssignment(assignment: FaceAssignmentEntity): Result<Unit>
    suspend fun deleteFace(faceId: String, schoolId: String, classIds: List<String>): Result<Unit>
}
