package com.azuratech.azuratime.data.local

import androidx.room.Embedded
import androidx.room.ColumnInfo

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
    val faceIsDeleted: Boolean? = null
)
