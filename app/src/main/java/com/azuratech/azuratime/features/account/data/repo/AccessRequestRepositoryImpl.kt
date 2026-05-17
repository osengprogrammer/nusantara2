package com.azuratech.azuratime.features.account.data.repo

import androidx.room.withTransaction
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AccessRequestRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val syncManager: SyncManager
) : AccessRequestRepository {

    private val accessRequestDao = database.accessRequestDao()

    override suspend fun submitRequest(userId: String, schoolId: String, schoolName: String): Result<Unit> {
        return try {
            val requestId = "req_${userId}_${schoolId}_${System.currentTimeMillis()}"
            database.withTransaction {
                accessRequestDao.insertRequest(AccessRequestEntity(
                    requestId = requestId,
                    requesterId = userId,
                    schoolId = schoolId,
                    schoolName = schoolName,
                    status = AccessRequestStatus.PENDING,
                    syncStatus = SyncStatus.PENDING_INSERT,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))
                syncManager.enqueueAccessSync(userId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun cancelRequest(requesterId: String, schoolId: String): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = accessRequestDao.getRequestByUserAndSchool(requesterId, schoolId)
                if (existing != null) {
                    accessRequestDao.insertRequest(existing.copy(
                        status = AccessRequestStatus.LEFT,
                        syncStatus = SyncStatus.PENDING_UPDATE,
                        updatedAt = System.currentTimeMillis()
                    ))
                    syncManager.enqueueAccessSync(requesterId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun observeRequestsByUser(userId: String): Flow<List<AccessRequestEntity>> {
        return accessRequestDao.observeRequestsByUser(userId)
    }

    override suspend fun getPendingRequests(schoolId: String): List<AccessRequestProfile> {
        // Basic implementation, can be refined
        return emptyList() 
    }

    override suspend fun approveRequest(requestId: String): Boolean {
        return try {
            val request = accessRequestDao.getRequestById(requestId)
            if (request != null) {
                accessRequestDao.insertRequest(request.copy(
                    status = AccessRequestStatus.APPROVED,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    updatedAt = System.currentTimeMillis()
                ))
                syncManager.enqueueAccessSync(request.requesterId)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun rejectRequest(requestId: String, reason: String): Boolean {
        return try {
            val request = accessRequestDao.getRequestById(requestId)
            if (request != null) {
                accessRequestDao.insertRequest(request.copy(
                    status = AccessRequestStatus.REJECTED,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    updatedAt = System.currentTimeMillis()
                ))
                syncManager.enqueueAccessSync(request.requesterId)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
