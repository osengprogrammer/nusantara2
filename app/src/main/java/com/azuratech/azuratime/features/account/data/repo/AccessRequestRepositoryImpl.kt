package com.azuratech.azuratime.features.account.data.repo

import androidx.room.withTransaction
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.core.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessRequestRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val syncManager: SyncManager,
) : AccessRequestRepository {

    private val accessRequestDao = database.accessRequestDao()

    override suspend fun submitRequest(accountId: String, schoolId: String, schoolName: String): Result<Unit> {
        return try {
            val requestId = "req_${accountId}_${schoolId}_${System.currentTimeMillis()}"
            database.withTransaction {
                accessRequestDao.insertRequest(
                    AccessRequestEntity(
                        requestId = requestId,
                        accountId = accountId,
                        schoolId = schoolId,
                        schoolName = schoolName,
                        status = AccessRequestStatus.PENDING,
                        syncStatus = SyncStatus.PENDING_INSERT,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.enqueueAccessSync(accountId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun cancelRequest(accountId: String, schoolId: String): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = accessRequestDao.getRequestByAccountAndSchool(accountId, schoolId)
                if (existing != null) {
                    accessRequestDao.insertRequest(
                        existing.copy(
                            status = AccessRequestStatus.LEFT,
                            syncStatus = SyncStatus.PENDING_UPDATE,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                    syncManager.enqueueAccessSync(accountId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun observeRequestsByAccount(accountId: String): Flow<List<AccessRequestEntity>> {
        return accessRequestDao.observeRequestsByAccount(accountId)
    }

    override suspend fun getPendingRequests(schoolId: String): List<AccessRequestProfile> {
        // Basic implementation, can be refined
        return emptyList()
    }

    override suspend fun approveRequest(requestId: String): Boolean {
        return try {
            val request = accessRequestDao.getRequestById(requestId)
            if (request != null) {
                accessRequestDao.insertRequest(
                    request.copy(
                        status = AccessRequestStatus.APPROVED,
                        syncStatus = SyncStatus.PENDING_UPDATE,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.enqueueAccessSync(request.accountId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun rejectRequest(requestId: String, reason: String): Boolean {
        return try {
            val request = accessRequestDao.getRequestById(requestId)
            if (request != null) {
                accessRequestDao.insertRequest(
                    request.copy(
                        status = AccessRequestStatus.REJECTED,
                        syncStatus = SyncStatus.PENDING_UPDATE,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                syncManager.enqueueAccessSync(request.accountId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
