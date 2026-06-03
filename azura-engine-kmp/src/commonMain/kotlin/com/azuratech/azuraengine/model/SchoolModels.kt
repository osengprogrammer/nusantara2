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
    val updatedAt: Long,
)

@Serializable
data class ClassModel(
    val id: String,
    val schoolId: String?,
    val name: String,
    val grade: String,
    val accountId: String?,
    val studentCount: Int = 0,
    val studentIds: List<String> = emptyList(), // 🔥 Class-centric assignment support
    val createdAt: Long,
)
