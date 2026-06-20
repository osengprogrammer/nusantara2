package com.azuratech.azuratime.features.template.domain.model

data class ClassTemplate(
    val id: String = "",
    val name: String = "",
    val level: Int = 0,
    val major: String = "",
    val section: String = "",
    val category: String = "",
    val active: Boolean = true,
)

fun com.google.firebase.firestore.DocumentSnapshot.toClassTemplate(): ClassTemplate? {
    return try {
        ClassTemplate(
            id = id,
            name = getString("name") ?: "",
            level = getLong("level")?.toInt() ?: 0,
            major = getString("major") ?: "",
            section = getString("section") ?: "",
            category = getString("category") ?: "",
            active = getBoolean("active") ?: true,
        )
    } catch (e: Exception) {
        null
    }
}
