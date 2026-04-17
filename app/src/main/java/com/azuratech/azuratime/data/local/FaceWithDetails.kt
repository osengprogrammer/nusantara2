package com.azuratech.azuratime.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * Data Class gabungan untuk menampilkan profil siswa lengkap dengan 
 * Kelas di layar manajemen tanpa banyak query manual.
 */
data class FaceWithDetails(
    @Embedded val face: FaceEntity,

    @ColumnInfo(name = "className")
    val className: String? = null,

    @ColumnInfo(name = "classId")
    val classId: String? = null
)