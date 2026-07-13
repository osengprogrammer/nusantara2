package com.azuratech.azuratime.core.domain.model

data class SubjectTemplate(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val active: Boolean = true,
)

fun com.google.firebase.firestore.DocumentSnapshot.toSubjectTemplate(): SubjectTemplate? {
    return try {
        SubjectTemplate(
            id = id,
            name = getString("name") ?: "",
            category = getString("category") ?: "",
            active = getBoolean("active") ?: true,
        )
    } catch (e: Exception) {
        null
    }
}
