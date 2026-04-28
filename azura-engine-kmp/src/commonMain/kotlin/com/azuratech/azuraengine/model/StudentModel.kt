package com.azuratech.azuraengine.model

import kotlinx.serialization.Serializable

@Serializable
data class StudentModel(
    val studentId: String,
    val schoolId: String,
    val name: String,
    val studentCode: String? = null,
    val classId: String? = null,
    val createdAt: Long = 0L,
    val isSynced: Boolean = false
)
