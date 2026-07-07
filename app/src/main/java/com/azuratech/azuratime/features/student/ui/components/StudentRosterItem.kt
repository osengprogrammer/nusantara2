package com.azuratech.azuratime.features.student.ui.components

data class StudentRosterItem(
    val studentId: String,
    val displayName: String,
    val studentCode: String?,
    val assignedClassNames: String,
    val isBiometricReady: Boolean,
    val currentBalance: Double = 0.0
) {
    fun formattedBalance(): String = "Rp %.0f".format(currentBalance)
}
