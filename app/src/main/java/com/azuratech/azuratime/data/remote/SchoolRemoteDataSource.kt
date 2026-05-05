package com.azuratech.azuratime.data.remote

import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow

interface SchoolRemoteDataSource {
    suspend fun saveSchool(accountId: String, school: School): Result<Unit>
    suspend fun deleteSchool(accountId: String, schoolId: String): Result<Unit>
    fun observeRemoteSchools(accountId: String): Flow<Result<List<School>>>
    suspend fun getSchools(accountId: String): Result<List<School>>
    suspend fun getSchoolsByIds(schoolIds: List<String>): Result<List<School>>

    suspend fun saveClass(accountId: String, schoolId: String, classModel: ClassModel): Result<Unit>
    suspend fun deleteClass(accountId: String, schoolId: String, classId: String): Result<Unit>
    suspend fun getClasses(accountId: String, schoolId: String): Result<List<ClassModel>>
}
