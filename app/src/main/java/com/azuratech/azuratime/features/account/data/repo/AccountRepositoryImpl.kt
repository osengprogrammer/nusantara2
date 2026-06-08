package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.toProfile
import com.azuratech.azuratime.features.account.data.local.toDomain
import com.azuratech.azuratime.features.account.data.local.SchoolMembership
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.AccountProfile
import com.azuratech.azuratime.features.school.data.local.SchoolDao
import com.azuratech.azuratime.core.sync.SyncManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚀 ACCOUNT REPOSITORY IMPLEMENTATION (v3.2.0-ai-native)
 * Local-First SSOT implementation for Account management.
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val schoolDao: SchoolDao,
    private val syncManager: SyncManager,
    private val db: FirebaseFirestore,
) : AccountRepository {

    override fun getAccountFlow(id: String): Flow<Result<Account>> =
        accountDao.observeAccountByIdFlow(id)
            .map { it?.toDomain() ?: throw Exception("Account not found in Local DB") }
            .asLocalResult()

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

    override fun observeAccountEntityFlow(id: String): Flow<Result<AccountEntity?>> =
        accountDao.observeAccountByIdFlow(id)
            .asLocalResult()

    override suspend fun getProfile(accountId: String): Result<AccountProfile> {
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
                val role = snapshot.getString("role") ?: "USER"
                val activeSchoolId = snapshot.getString("activeSchoolId")
                val activeClassId = snapshot.getString("activeClassId")

                val membershipsRaw = snapshot.data?.get("memberships") as? Map<*, *>
                val memberships = membershipsRaw?.mapNotNull { (key, value) ->
                    val k = key as? String ?: return@mapNotNull null
                    val v = value as? Map<*, *> ?: return@mapNotNull null
                    k to SchoolMembership(
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

    override suspend fun searchAccounts(email: String): Result<List<AccountEntity>> {
        return try {
            val snapshot = db.collection("accounts")
                .whereEqualTo("email", email.lowercase().trim())
                .limit(5)
                .get()
                .await()

            val accounts = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: ""
                val mail = doc.getString("email") ?: ""
                val photo = doc.getString("photoUrl")

                AccountEntity(
                    accountId = id,
                    email = mail,
                    name = name,
                    photoUrl = photo,
                    status = doc.getString("status") ?: "ACTIVE",
                    role = doc.getString("role") ?: "USER",
                )
            }
            Result.Success(accounts)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun followAccount(accountId: String, targetAccountId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            val senderRef = db.collection("whitelisted_accounts").document(accountId)
            val targetRef = db.collection("whitelisted_accounts").document(targetAccountId)

            batch.set(
                senderRef,
                mapOf(
                    "followingIds" to com.google.firebase.firestore.FieldValue.arrayUnion(targetAccountId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.set(
                targetRef,
                mapOf(
                    "followerIds" to com.google.firebase.firestore.FieldValue.arrayUnion(accountId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun unfollowAccount(accountId: String, targetAccountId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            val senderRef = db.collection("whitelisted_accounts").document(accountId)
            val targetRef = db.collection("whitelisted_accounts").document(targetAccountId)

            batch.set(
                senderRef,
                mapOf(
                    "followingIds" to com.google.firebase.firestore.FieldValue.arrayRemove(targetAccountId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.set(
                targetRef,
                mapOf(
                    "followerIds" to com.google.firebase.firestore.FieldValue.arrayRemove(accountId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun updateFcmToken(accountId: String, token: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.set(db.collection("accounts").document(accountId), mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            batch.set(db.collection("whitelisted_accounts").document(accountId), mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun sendConnectionRequest(senderId: String, targetId: String): Result<Unit> {
        return try {
            val request = mapOf(
                "senderId" to senderId,
                "targetId" to targetId,
                "status" to "PENDING",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            db.collection("connection_requests").document("${senderId}_$targetId").set(request).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun acceptConnectionRequest(targetId: String, senderId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.delete(db.collection("connection_requests").document("${senderId}_$targetId"))

            val senderRef = db.collection("whitelisted_accounts").document(senderId)
            val targetRef = db.collection("whitelisted_accounts").document(targetId)

            batch.set(
                senderRef,
                mapOf(
                    "followingIds" to com.google.firebase.firestore.FieldValue.arrayUnion(targetId),
                    "followerIds" to com.google.firebase.firestore.FieldValue.arrayUnion(targetId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.set(
                targetRef,
                mapOf(
                    "followingIds" to com.google.firebase.firestore.FieldValue.arrayUnion(senderId),
                    "followerIds" to com.google.firebase.firestore.FieldValue.arrayUnion(senderId),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )

            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun declineConnectionRequest(targetId: String, senderId: String): Result<Unit> {
        return try {
            db.collection("connection_requests").document("${senderId}_$targetId").delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override fun observePendingRequestsFlow(accountId: String): Flow<Result<List<AccountEntity>>> {
        return callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("targetId", accountId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Failure(AppError.Network(error.message)))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val senderIds = snapshot.documents.mapNotNull { it.getString("senderId") }
                        if (senderIds.isEmpty()) {
                            trySend(Result.Success(emptyList()))
                            return@addSnapshotListener
                        }
                        db.collection("accounts").whereIn(com.google.firebase.firestore.FieldPath.documentId(), senderIds).get()
                            .addOnSuccessListener { accountSnap ->
                                val accounts = accountSnap.documents.map { doc ->
                                    val membershipsRaw = doc.data?.get("memberships") as? Map<*, *>
                                    val memberships = membershipsRaw?.mapNotNull { (key, value) ->
                                        val k = key as? String ?: return@mapNotNull null
                                        val v = value as? Map<*, *> ?: return@mapNotNull null
                                        k to SchoolMembership(
                                            schoolName = v["schoolName"] as? String ?: "",
                                            role = v["role"] as? String ?: "USER",
                                            status = v["status"] as? String ?: "ACTIVE",
                                            assignedClassIds = (v["assignedClassIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        )
                                    }?.toMap() ?: emptyMap()

                                    AccountEntity(
                                        accountId = doc.id,
                                        email = doc.getString("email") ?: "",
                                        name = doc.getString("name") ?: "",
                                        photoUrl = doc.getString("photoUrl"),
                                        status = "ACTIVE",
                                        memberships = memberships,
                                        role = doc.getString("role") ?: "USER",
                                    )
                                }
                                trySend(Result.Success(accounts))
                            }
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    override fun observeSentRequestsFlow(accountId: String): Flow<Result<List<String>>> {
        return callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("senderId", accountId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Failure(AppError.Network(error.message)))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val targetIds = snapshot.documents.mapNotNull { it.getString("targetId") }
                        trySend(Result.Success(targetIds))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    override fun observePendingRequestsCountFlow(accountId: String): Flow<Int> {
        return callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("targetId", accountId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        trySend(snapshot.size())
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    override fun observeConnectionsFlow(accountId: String): Flow<Result<List<AccountEntity>>> {
        return callbackFlow {
            val listener = db.collection("whitelisted_accounts").document(accountId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.Failure(AppError.Network(error.message)))
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val connectedIds = snapshot.get("followingIds") as? List<String> ?: emptyList()
                        if (connectedIds.isEmpty()) {
                            trySend(Result.Success(emptyList()))
                            return@addSnapshotListener
                        }
                        db.collection("accounts").whereIn(com.google.firebase.firestore.FieldPath.documentId(), connectedIds).get()
                            .addOnSuccessListener { accountSnap ->
                                val accounts = accountSnap.documents.map { doc ->
                                    val membershipsRaw = doc.data?.get("memberships") as? Map<*, *>
                                    val memberships = membershipsRaw?.mapNotNull { (key, value) ->
                                        val k = key as? String ?: return@mapNotNull null
                                        val v = value as? Map<*, *> ?: return@mapNotNull null
                                        k to SchoolMembership(
                                            schoolName = v["schoolName"] as? String ?: "",
                                            role = v["role"] as? String ?: "USER",
                                            status = v["status"] as? String ?: "ACTIVE",
                                            assignedClassIds = (v["assignedClassIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        )
                                    }?.toMap() ?: emptyMap()

                                    AccountEntity(
                                        accountId = doc.id,
                                        email = doc.getString("email") ?: "",
                                        name = doc.getString("name") ?: "",
                                        photoUrl = doc.getString("photoUrl"),
                                        status = "ACTIVE",
                                        memberships = memberships,
                                        role = doc.getString("role") ?: "USER",
                                    )
                                }
                                trySend(Result.Success(accounts))
                            }
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun assignClassToConnection(targetId: String, schoolId: String, classIds: List<String>): Result<Unit> {
        return try {
            val doc = db.collection("whitelisted_accounts").document(targetId).get().await()
            if (!doc.exists()) return Result.Failure(AppError.BusinessRule("Akun tidak ditemukan"))

            @Suppress("UNCHECKED_CAST")
            val memberships = doc.get("memberships") as? MutableMap<String, Any> ?: mutableMapOf()

            @Suppress("UNCHECKED_CAST")
            val schoolMembership = memberships[schoolId] as? MutableMap<String, Any> ?: mutableMapOf()

            schoolMembership["assignedClassIds"] = classIds
            schoolMembership["status"] = "ACTIVE"
            schoolMembership["role"] = "SUPERVISOR"
            memberships[schoolId] = schoolMembership

            db.collection("whitelisted_accounts").document(targetId).update("memberships", memberships).await()
            db.collection("accounts").document(targetId).update("memberships", memberships).await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun updateMemberRole(targetAccountId: String, schoolId: String, newRole: com.azuratech.azuratime.core.domain.model.AccountRole): Result<Unit> {
        return try {
            val account = accountDao.getAccountById(targetAccountId) ?: return Result.Failure(AppError.LocalDB("Account not found"))

            val updatedMemberships = account.memberships.toMutableMap()

            // 🛡️ AI Native: Auto-Enroll logic if membership doesn't exist
            val existingMembership = updatedMemberships[schoolId]
            if (existingMembership == null) {
                val school = schoolDao.getSchoolById(schoolId)
                val newMembership = SchoolMembership(
                    schoolName = school?.name ?: "Unknown School",
                    role = newRole.name,
                    status = "ACTIVE",
                )
                updatedMemberships[schoolId] = newMembership
            } else {
                updatedMemberships[schoolId] = existingMembership.copy(role = newRole.name)
            }

            // If this is the active school, update the top-level role too
            val updatedRole = if (account.activeSchoolId == schoolId) newRole.name else account.role

            val updatedAccount = account.copy(
                memberships = updatedMemberships,
                role = updatedRole,
                syncStatus = "PENDING_UPDATE",
            )

            accountDao.upsertAccount(updatedAccount)

            // ⚡ AI Native: Trigger sync via SyncManager for background resilience
            syncManager.enqueueAccountSync(targetAccountId)

            // Also push immediately for rapid UI updates if online
            pushAccount(targetAccountId).onSuccess {
                accountDao.upsertAccount(updatedAccount.copy(syncStatus = "SYNCED"))
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
