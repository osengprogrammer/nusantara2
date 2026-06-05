package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.domain.model.SchoolMembership
import kotlinx.coroutines.flow.Flow

sealed class MembershipDocUpdate {
    data class StatusChanged(val status: String, val data: Map<String, Any>, val isoKey: String?) : MembershipDocUpdate()
    object DocumentMissing : MembershipDocUpdate()
}

interface MembershipRepository {
    fun getCurrentUid(): Result<String?>
    suspend fun checkWhitelisted(uid: String): Result<Map<String, Any>?>
    suspend fun checkMembershipExists(uid: String): Result<Boolean>
    suspend fun createPendingAccount(uid: String, email: String, displayName: String?): Result<Unit>
    fun savePendingStatus(): Result<Unit>
    fun activateSession(data: Map<String, Any>?): Result<Boolean>
    fun observeMembershipsFlow(uid: String): Flow<Result<List<SchoolMembership>>>
    fun observeMembershipFlow(uid: String): Flow<Result<MembershipDocUpdate>>
    suspend fun pollWhitelistedFinal(uid: String): Result<Map<String, Any>?>
}
