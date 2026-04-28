package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuraengine.model.StudentModel

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["schoolId"]),
        Index(value = ["classId"])
    ]
)
data class StudentEntity(
    @PrimaryKey
    val studentId: String,
    val schoolId: String,
    val name: String,
    val studentCode: String? = null,
    val classId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun toDomain() = StudentModel(
        studentId = studentId,
        schoolId = schoolId,
        name = name,
        studentCode = studentCode,
        classId = classId,
        createdAt = createdAt,
        isSynced = isSynced
    )
}
