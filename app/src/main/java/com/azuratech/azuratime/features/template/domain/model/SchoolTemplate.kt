package com.azuratech.azuratime.features.template.domain.model

import android.util.Log

data class SchoolTemplate(
    val id: String = "",
    val name: String = "",
    val category: String = "", // "SD", "SMP", "SMA", etc.
    val description: String = "",
    val defaultClassIds: List<String> = emptyList(),
    val defaultSubjectIds: List<String> = emptyList(),
    val isActive: Boolean = true,
)

fun com.google.firebase.firestore.DocumentSnapshot.toSchoolTemplate(): SchoolTemplate? {
    return try {
        val name = getString("name") ?: ""
        val category = getString("category") ?: ""
        val description = getString("description") ?: ""
        val isActive = getBoolean("isActive") ?: true

        val defaultClassIdsRaw = get("defaultClassIds")
        val defaultClassIds = if (defaultClassIdsRaw != null) {
            val list = defaultClassIdsRaw as? List<*>
            if (list == null) {
                Log.e("TemplateParser", "Dokumen $id: defaultClassIds bukan List melainkan ${defaultClassIdsRaw.javaClass.name} ($defaultClassIdsRaw)")
            }
            list?.mapNotNull { it as? String } ?: emptyList()
        } else {
            emptyList()
        }

        val defaultSubjectIdsRaw = get("defaultSubjectIds")
        val defaultSubjectIds = if (defaultSubjectIdsRaw != null) {
            val list = defaultSubjectIdsRaw as? List<*>
            if (list == null) {
                Log.e("TemplateParser", "Dokumen $id: defaultSubjectIds bukan List melainkan ${defaultSubjectIdsRaw.javaClass.name} ($defaultSubjectIdsRaw)")
            }
            list?.mapNotNull { it as? String } ?: emptyList()
        } else {
            emptyList()
        }

        SchoolTemplate(
            id = id,
            name = name,
            category = category,
            description = description,
            defaultClassIds = defaultClassIds,
            defaultSubjectIds = defaultSubjectIds,
            isActive = isActive,
        )
    } catch (e: Exception) {
        Log.e("TemplateParser", "Gagal mapping dokumen ${this.id}: ${e.message}", e)
        null
    }
}
