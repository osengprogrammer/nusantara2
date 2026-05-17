package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.toProfile
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val db: FirebaseFirestore,
) : AccountRepository {
    override fun getAccountDao() = accountDao

    override suspend fun getAccountById(id: String): Result<AccountEntity> {
        return try {
            val account = accountDao.getAccountById(id)
            if (account != null) {
                Result.Success(account)
            } else {
                Result.Failure(AppError.LocalDB("Account not found"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun observeAccountEntity(id: String) = accountDao.observeAccountById(id)

    override suspend fun getProfile(accountId: String): Result<com.azuratech.azuratime.features.account.domain.model.AccountProfile> {
        return syncAccount(accountId).map { it.toProfile() }
    }

    override suspend fun updateDisplayName(accountId: String, newName: String): Result<Unit> {
        return try {
            val accountResult = getAccountById(accountId)
            if (accountResult is Result.Failure) return Result.Failure(accountResult.error)
            val account = (accountResult as Result.Success).data
            val updated = account.copy(name = newName)
            accountDao.upsertAccount(updated)
            pushAccount(accountId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updatePhoto(accountId: String, photoUrl: String): Result<Unit> {
        return try {
            val accountResult = getAccountById(accountId)
            if (accountResult is Result.Failure) return Result.Failure(accountResult.error)
            val account = (accountResult as Result.Success).data
            val updated = account.copy(photoUrl = photoUrl)
            accountDao.upsertAccount(updated)
            pushAccount(accountId)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncAccount(accountId: String): Result<AccountEntity> {
        return try {
            val snapshot = db.collection("accounts").document(accountId).get().await()
            if (snapshot.exists()) {
                val email = snapshot.getString("email") ?: ""
                val name = snapshot.getString("name") ?: ""
                val photoUrl = snapshot.getString("photoUrl")
                val status = snapshot.getString("status") ?: "PENDING"
                val role = snapshot.getString("role") ?: "MEMBER"
                val activeSchoolId = snapshot.getString("activeSchoolId")
                val activeClassId = snapshot.getString("activeClassId")

                val membershipsRaw = snapshot.data?.get("memberships") as? Map<*, *>
                val memberships = membershipsRaw?.mapNotNull { (key, value) ->
                    val k = key as? String ?: return@mapNotNull null
                    val v = value as? Map<*, *> ?: return@mapNotNull null
                    k to Membership(
                        schoolName = v["schoolName"] as? String ?: "",
                        role = v["role"] as? String ?: "MEMBER",
                        status = v["status"] as? String ?: "ACTIVE",
                        assignedClassIds = (v["assignedClassIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    )
                }?.toMap() ?: emptyMap()

                val entity = AccountEntity(
                    accountId = accountId,
                    email = email,
                    name = name,
                    photoUrl = photoUrl,
                    status = status,
                    role = role,
                    activeSchoolId = activeSchoolId,
                    activeClassId = activeClassId,
                    memberships = memberships,
                )
                accountDao.upsertAccount(entity)
                Result.Success(entity)
            } else {
                Result.Failure(AppError.BusinessRule("Account not found in Cloud"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun pushAccount(accountId: String): Result<Unit> {
        return try {
            val accountResult = getAccountById(accountId)
            if (accountResult is Result.Failure) return Result.Failure(accountResult.error)
            val account = (accountResult as Result.Success).data
            val data = mapOf(
                "accountId" to account.accountId,
                "email" to account.email,
                "name" to account.name,
                "photoUrl" to account.photoUrl,
                "status" to account.status,
                "role" to account.role,
                "activeSchoolId" to account.activeSchoolId,
                "activeClassId" to account.activeClassId,
                "memberships" to account.memberships,
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            db.collection("accounts").document(accountId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
