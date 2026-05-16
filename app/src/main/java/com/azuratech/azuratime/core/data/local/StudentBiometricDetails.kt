package com.azuratech.azuratime.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity

/**
 * Data Class gabungan untuk menampilkan profil siswa lengkap dengan 
 * Kelas di layar manajemen tanpa banyak query manual.
 */
data class StudentBiometricDetails(
    @Embedded val biometric: StudentBiometricEntity,

    @ColumnInfo(name = "className")
    val className: String? = null,

    @ColumnInfo(name = "classId")
    val classId: String? = null,

    @Ignore
    val classIds: List<String> = emptyList()
) {
    // Required secondary constructor for Room to use when not providing @Ignore fields
    constructor(biometric: StudentBiometricEntity, className: String?, classId: String?) : this(biometric, className, classId, emptyList())
}
