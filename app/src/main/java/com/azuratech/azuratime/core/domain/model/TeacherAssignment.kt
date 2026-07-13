package com.azuratech.azuratime.core.domain.model
import com.azuratech.azuratime.core.domain.model.TeacherAssignment

/**
 * 🎓 TEACHER ASSIGNMENT (v3.4.0-matrix)
 * Represents a matrix pairing of a Class and an optional Subject.
 * If subjectId is null, the teacher is a "Wali Kelas" (Homeroom Teacher).
 */
data class TeacherAssignment(
    val classId: String,
    val subjectId: String? = null,
)
