package com.azuratech.azuratime.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.azuratech.azuratime.features.biometric.data.local.StudentClassAssignmentEntity
import com.azuratech.azuratime.features.student.data.local.StudentEntity

/**
 * 🛠️ RAW STUDENT PROFILE
 * Intermediate data class for Room JOIN queries between students and faces.
 * Internal to the data layer.
 */
data class RawStudentProfile(
    @Embedded val student: StudentEntity,
    
    @ColumnInfo(name = "faceId")
    val faceId: String? = null,
    
    @ColumnInfo(name = "embedding")
    val embedding: FloatArray? = null,
    
    @ColumnInfo(name = "photoUrl")
    val photoUrl: String? = null,
    
    @ColumnInfo(name = "faceLastUpdated")
    val faceLastUpdated: Long? = null,
    
    @ColumnInfo(name = "faceIsSynced")
    val faceIsSynced: Boolean? = null,
    
    @ColumnInfo(name = "faceIsDeleted")
    val faceIsDeleted: Boolean? = null,

    /**
     * 🔗 Multi-class assignments fetched via relation.
     * Note: This requires @Transaction on the DAO query.
     */
    @Relation(
        parentColumn = "faceId",
        entityColumn = "studentId"
    )
    val assignments: List<StudentClassAssignmentEntity> = emptyList()
) {
    /**
     * Extracts all unique class IDs from the assignments and the primary student entity.
     */
    val allClassIds: List<String> 
        get() = (assignments.map { it.classId } + listOfNotNull(student.classId)).distinct()
}
