package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.Membership
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val db: FirebaseFirestore,
) {
    fun getAccountDao() = accountDao

    suspend fun getAccountById(id: String): AccountEntity? = accountDao.getAccountById(id)

    fun observeAccountEntity(id: String) = accountDao.observeAccountById(id)

    suspend fun syncAccount(accountId: String): Result<AccountEntity> {
        return try {
            val snapshot = db.collection("accounts").document(accountId).get().await()
            if (snapshot.exists()) {
                val email = snapshot.getString("email") ?: ""
                val name = snapshot.getString("name") ?: ""
                val photoUrl = snapshot.getString("photoUrl")
                val status = snapshot.getString("status") ?: "PENDING"
                val role = snapshot.getString("role") ?: "USER"
                val activeSchoolId = snapshot.getString("activeSchoolId")
                val activeClassId = snapshot.getString("activeClassId")

                val membershipsRaw = snapshot.data?.get("memberships") as? Map<*, *>
                val memberships = membershipsRaw?.mapNotNull { (key, value) ->
                    val k = key as? String ?: return@mapNotNull null
                    val v = value as? Map<*, *> ?: return@mapNotNull null
                    k to Membership(
                        schoolName = v["schoolName"] as? String ?: "",
                        role = v["role"] as? String ?: "USER",
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

    suspend fun pushAccount(accountId: String): Result<Unit> {
        return try {
            val account = accountDao.getAccountById(accountId) ?: return Result.Failure(AppError.LocalDB("Local account not found"))
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
