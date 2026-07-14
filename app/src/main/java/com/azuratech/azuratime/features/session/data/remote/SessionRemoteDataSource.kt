package com.azuratech.azuratime.features.session.data.remote

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.core.data.local.SubjectEntity

interface SessionRemoteDataSource {
    suspend fun getSubjectUpdates(schoolId: String): Result<List<SubjectEntity>>
    suspend fun syncSubject(subject: SubjectEntity): Result<Unit>
    suspend fun deleteSubject(schoolId: String, subjectId: String): Result<Unit>

    suspend fun getSessionUpdates(schoolId: String): Result<List<ClassSessionEntity>>
    suspend fun syncSession(session: ClassSessionEntity): Result<Unit>
    suspend fun deleteSession(schoolId: String, sessionId: String): Result<Unit>
}
