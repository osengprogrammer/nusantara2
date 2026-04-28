package com.azuratech.azuraengine.model

import kotlinx.serialization.Serializable

@Serializable
data class School(
    val id: String,
    val accountId: String,
    val name: String,
    val timezone: String,
    val status: String = "ACTIVE",
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ClassModel(
    val id: String,
    val schoolId: String?,
    val name: String,
    val grade: String,
    val teacherId: String?,
    val studentCount: Int = 0,
    val createdAt: Long
)
