package com.azuratech.azuratime.features.account.data.repo
import com.azuratech.azuratime.core.data.local.SchoolMembership
import com.azuratech.azuratime.core.data.local.AccountEntity
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.core.data.local.toDomain
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.account.domain.model.SchoolMembership as DomainSchoolMembership
import com.azuratech.azuratime.features.account.domain.repository.MembershipRepository
import com.azuratech.azuratime.features.account.domain.repository.MembershipDocUpdate
import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.result.AppError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class MembershipRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionManager: SessionManager,
    private val accountDao: AccountDao,
    private val syncManager: SyncManager,
    private val accountRepository: com.azuratech.azuratime.features.account.domain.repository.AccountRepository,
) : MembershipRepository {
    override fun getCurrentUid(): Result<String?> = Result.Success(firebaseAuth.currentUser?.uid)

    override suspend fun checkWhitelisted(uid: String): Result<Map<String, Any>?> = withContext(Dispatchers.IO) {
        try {
            val account = accountDao.getAccountById(uid)
            if (account != null && account.status == SessionManager.STATUS_ACTIVE) {
                return@withContext Result.Success(accountToMap(account))
            }

            accountRepository.syncAccount(uid)

            val refreshedAccount = accountDao.getAccountById(uid)
            if (refreshedAccount != null && refreshedAccount.status == SessionManager.STATUS_ACTIVE) {
                Result.Success(accountToMap(refreshedAccount))
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun checkMembershipExists(uid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val account = accountDao.getAccountById(uid)
            if (account != null) return@withContext Result.Success(true)

            accountRepository.syncAccount(uid)
            Result.Success(accountDao.getAccountById(uid) != null)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun createPendingAccount(uid: String, email: String, displayName: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = AccountEntity(
                accountId = uid,
                email = email,
                name = displayName ?: "Account",
                role = "USER", // 🔥 AI Native Secure Default
                status = SessionManager.STATUS_PENDING,
                syncStatus = SyncStatus.PENDING_UPDATE.name,
            )
            accountDao.upsertAccount(account)

            val hardwareId = sessionManager.getHardwareId()
            val pendingData = mapOf(
                "accountId" to uid,
                "email" to email,
                "name" to (displayName ?: "Account"),
                "hardwareId" to hardwareId,
                "status" to "PENDING",
                "role" to "USER", // 🔥 AI Native Secure Default
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            firestore.collection("memberships").document(uid).set(pendingData, com.google.firebase.firestore.SetOptions.merge()).await()

            sessionManager.saveAccountStatus(SessionManager.STATUS_PENDING)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun savePendingStatus(): Result<Unit> {
        return try {
            sessionManager.saveAccountStatus(SessionManager.STATUS_PENDING)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    override fun activateSession(data: Map<String, Any>?): Result<Boolean> {
        return try {
            val isoKey = data?.get("secureIsoKey")?.toString() ?: ""
            val schoolId = data?.get("activeSchoolId")?.toString() ?: data?.get("schoolId")?.toString() ?: ""

            val expireDate = (data?.get("expireDate") as? Number)?.toLong()
                ?: (System.currentTimeMillis() + 31536000000L)

            if (schoolId.isNotEmpty()) {
                sessionManager.saveActiveSchoolId(schoolId)
            }

            sessionManager.saveAccountStatus(SessionManager.STATUS_ACTIVE)

            if (!isoKey.isNullOrEmpty()) {
                sessionManager.injectSecurityEnvelope(isoKey, expireDate)
            }
            Result.Success(true)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    override fun observeMembershipsFlow(uid: String): Flow<Result<List<DomainSchoolMembership>>> {
        return accountDao.observeAccountByIdFlow(uid).map { account ->
            Result.Success(account?.memberships?.values?.map { it.toDomain() } ?: emptyList())
        }
    }

    override fun observeMembershipFlow(uid: String): Flow<Result<MembershipDocUpdate>> {
        return accountDao.observeAccountByIdFlow(uid).map { account ->
            if (account == null) {
                Result.Success(MembershipDocUpdate.DocumentMissing)
            } else {
                Result.Success(MembershipDocUpdate.StatusChanged(account.status, accountToMap(account), null))
            }
        }
    }

    override suspend fun pollWhitelistedFinal(uid: String): Result<Map<String, Any>?> = withContext(Dispatchers.IO) {
        try {
            var retryCount = 0
            while (retryCount < 5) {
                accountRepository.syncAccount(uid)
                val account = accountDao.getAccountById(uid)
                if (account != null && account.status == SessionManager.STATUS_ACTIVE) {
                    return@withContext Result.Success(accountToMap(account))
                }
                delay(2000)
                retryCount++
            }
            Result.Success(null)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    private fun accountToMap(account: AccountEntity): Map<String, Any> {
        return mapOf(
            "accountId" to account.accountId,
            "email" to account.email,
            "name" to account.name,
            "status" to account.status,
            "activeSchoolId" to (account.activeSchoolId ?: ""),
            "role" to account.role,
            "memberships" to account.memberships,
        )
    }
}
