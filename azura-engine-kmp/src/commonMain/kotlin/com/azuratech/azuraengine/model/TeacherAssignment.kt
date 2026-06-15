package com.azuratech.azuraengine.model

import kotlinx.serialization.Serializable

/**
 * 🎓 TEACHER ASSIGNMENT (v3.4.0-matrix)
 * Represents a matrix pairing of a Class and an optional Subject.
 */
@Serializable
data class TeacherAssignment(
    val classId: String,
    val subjectId: String? = null,
)
