package com.azuratech.azuratime.features.school.data.remote
import com.azuratech.azuratime.core.data.local.GpsGeofenceEntity
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.flow.Flow



interface SchoolRemoteDataSource {
    suspend fun saveSchool(accountId: String, school: School): Result<Unit>
    suspend fun deleteSchool(accountId: String, schoolId: String): Result<Unit>
    fun observeRemoteSchoolsFlow(accountId: String): Flow<Result<List<School>>>
    suspend fun getSchools(accountId: String): Result<List<School>>
    suspend fun getSchoolsByIds(schoolIds: List<String>): Result<List<School>>

    suspend fun saveClass(_accountId: String, schoolId: String, classModel: ClassModel): Result<Unit>
    suspend fun deleteClass(_accountId: String, schoolId: String, classId: String): Result<Unit>
    suspend fun getClasses(_accountId: String, schoolId: String): Result<List<ClassModel>>
    suspend fun addStudentToClass(schoolId: String, classId: String, studentId: String): Result<Unit>

    // 📍 GPS GEOFENCE
    suspend fun saveGeofence(schoolId: String, geofence: com.azuratech.azuratime.core.data.local.GpsGeofenceEntity): Result<Unit>
    fun observeGeofenceFlow(schoolId: String): Flow<Result<com.azuratech.azuratime.core.data.local.GpsGeofenceEntity?>>
}
