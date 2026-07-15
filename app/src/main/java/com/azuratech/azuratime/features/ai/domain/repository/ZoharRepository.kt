package com.azuratech.azuratime.features.ai.domain.repository

import com.azuratech.azuratime.core.result.Result

interface ZoharRepository {
    suspend fun generateAttendanceInsight(schoolId: String): Result<String>
    suspend fun askZohar(question: String, schoolId: String): Result<String>
}
