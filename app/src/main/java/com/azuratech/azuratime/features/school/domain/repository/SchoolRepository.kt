package com.azuratech.azuratime.features.school.domain.repository

import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import com.azuratech.azuratime.features.school.data.local.GpsGeofenceEntity
import kotlinx.coroutines.flow.Flow

interface SchoolRepository {
    fun observeSchoolsFlow(accountId: String): Flow<Result<List<School>>>
    fun observeSchoolsByIdsFlow(schoolIds: List<String>): Flow<Result<List<School>>>
    fun observeSchoolByIdFlow(id: String): Flow<Result<School?>>
    fun observeAllSchoolsFlow(): Flow<Result<List<School>>>

    /**
     * 🔥 ASSIGNED SCHOOL: School workspace operations.
     */
    suspend fun createSchool(adminId: String, name: String, timezone: String): Result<String>
    suspend fun updateSchoolDetails(schoolId: String, name: String?, timezone: String?): Result<Unit>
    suspend fun saveSchool(school: School): Result<Unit>
    suspend fun saveSchoolLocally(school: School): Result<Unit>
    suspend fun getSchoolById(id: String): Result<School>
    suspend fun getCountByAccount(accountId: String): Result<Int>
    suspend fun getFirstSchoolId(accountId: String): Result<String?>
    suspend fun schoolExists(schoolId: String): Result<Boolean>
    suspend fun syncSchools(schoolIds: List<String>): Result<Unit>
    suspend fun syncClasses(accountId: String, schoolId: String): Result<Unit>
    suspend fun deleteSchool(id: String, accountId: String): Result<Unit>

    // 🏫 ASSIGNED SCHOOL: CLASS OPERATIONS
    fun observeClassesFlow(schoolId: String): Flow<Result<List<ClassModel>>>
    suspend fun getClasses(schoolId: String): Result<List<ClassModel>>
    suspend fun saveClass(_accountId: String, schoolId: String?, classModel: ClassModel): Result<Unit>
    suspend fun assignClassToSchool(schoolId: String, classId: String): Result<Unit>
    suspend fun unassignClassFromSchool(schoolId: String, classId: String): Result<Unit>
    suspend fun getAssignedClassIds(schoolId: String): Result<List<String>>
    suspend fun deleteClass(_accountId: String, schoolId: String, classId: String): Result<Unit>
    suspend fun saveClassLocally(classEntity: ClassEntity): Result<Unit>
    fun getLocalClassesFlow(schoolId: String): Flow<Result<List<ClassEntity>>>
    fun observeAllClassesForAccountFlow(accountId: String): Flow<Result<List<ClassModel>>>
    suspend fun reassignClass(accountId: String, classId: String, newSchoolId: String): Result<Unit>
    suspend fun getOrphanedClasses(): Result<List<ClassModel>>
    suspend fun updateClassSchool(classId: String, schoolId: String): Result<Unit>
    suspend fun approveSchool(schoolId: String): Result<Unit>
    suspend fun rejectSchool(schoolId: String, reason: String): Result<Unit>
    suspend fun pushSchool(schoolId: String): Result<Unit>
    suspend fun pushAccessRequests(accountId: String): Result<Unit>

    suspend fun getClassById(id: String): Result<ClassModel?>

    // 🔥 CLASS-CENTRIC ASSIGNMENT
    suspend fun addStudentToClass(schoolId: String, classId: String, studentId: String): Result<Unit>

    // 📍 GPS GEOFENCE
    suspend fun saveGeofence(schoolId: String, lat: Double, lng: Double, radius: Int, isActive: Boolean): Result<Unit>
    fun observeGeofenceFlow(schoolId: String): Flow<GpsGeofenceEntity?>
}
