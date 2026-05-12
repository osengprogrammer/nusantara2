package com.azuratech.azuratime.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity

/**
 * Data Class gabungan untuk menampilkan profil siswa lengkap dengan 
 * Kelas di layar manajemen tanpa banyak query manual.
 */
data class FaceWithDetails(
    @Embedded val face: BiometricFaceEntity,

    @ColumnInfo(name = "className")
    val className: String? = null,

    @ColumnInfo(name = "classId")
    val classId: String? = null,

    @Ignore
    val classIds: List<String> = emptyList()
) {
    // Required secondary constructor for Room to use when not providing @Ignore fields
    constructor(face: BiometricFaceEntity, className: String?, classId: String?) : this(face, className, classId, emptyList())
}