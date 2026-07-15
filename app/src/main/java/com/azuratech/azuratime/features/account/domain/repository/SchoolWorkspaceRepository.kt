package com.azuratech.azuratime.features.account.domain.repository

import com.azuratech.azuratime.core.result.Result

interface SchoolWorkspaceRepository {
    suspend fun searchSchools(query: String): Result<List<Map<String, Any>>>
    suspend fun createNewSchool(accountId: String, schoolName: String): Result<String>
    suspend fun finalizeSetup(schoolId: String): Result<Unit>
    suspend fun updateSchoolName(schoolId: String, newName: String): Result<Unit>
    suspend fun switchWorkspace(accountId: String, newSchoolId: String): Result<Unit>
    suspend fun assignSchoolRole(accountId: String, schoolId: String, role: String, schoolName: String): Result<Unit>
    suspend fun requestToJoinWorkspace(accountId: String, schoolId: String, schoolName: String): Result<Unit>
}
