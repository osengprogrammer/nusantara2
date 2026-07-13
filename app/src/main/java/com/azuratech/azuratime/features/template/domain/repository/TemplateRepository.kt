package com.azuratech.azuratime.features.template.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.model.ClassTemplate
import com.azuratech.azuratime.features.template.domain.model.SubjectTemplate

/**
 * 🛠️ TEMPLATE REPOSITORY (v3.4.0)
 * Handles high-efficiency Firestore-to-Room synchronization for school templates.
 */
interface TemplateRepository {
    /**
     * 🔥 Firestore Batch Fetch: Pull multiple classes by IDs using 'whereIn'.
     * Minimizes network requests by fetching up to 10-30 IDs at once.
     */
    suspend fun fetchClassesByIds(schoolId: String, classIds: List<String>): Result<List<ClassEntity>>

    /**
     * 🔥 Firestore Batch Fetch: Pull multiple subjects by IDs using 'whereIn'.
     */
    suspend fun fetchSubjectsByIds(schoolId: String, subjectIds: List<String>): Result<List<SubjectEntity>>

    /**
     * 🔥 Atomic Sync: Persist template data to Room with OnConflictStrategy.IGNORE.
     */
    suspend fun persistTemplateData(classes: List<ClassEntity>, subjects: List<SubjectEntity>): Result<Unit>

    /**
     * 🔥 Firestore Fetch: Get all available school templates.
     */
    suspend fun fetchSchoolTemplates(): Result<List<SchoolTemplate>>

    /**
     * 🔥 Firestore Batch Fetch: Get global classes templates by their IDs.
     */
    suspend fun fetchGlobalClassesByIds(classIds: List<String>): Result<List<ClassTemplate>>

    /**
     * 🔥 Firestore Batch Fetch: Get global subjects templates by their IDs.
     */
    suspend fun fetchGlobalSubjectsByIds(subjectIds: List<String>): Result<List<SubjectTemplate>>

    /**
     * 🔥 Firestore Fetch: Get all global classes templates.
     */
    suspend fun fetchAllGlobalClasses(): Result<List<ClassTemplate>>

    /**
     * 🔥 Firestore Fetch: Get all global subjects templates.
     */
    suspend fun fetchAllGlobalSubjects(): Result<List<SubjectTemplate>>

    /**
     * 🔥 Atomic Apply: Fetch templates, map to entities, and persist atomically.
     * Keeps UseCase clean from data layer entities.
     */
    suspend fun applyTemplate(schoolId: String, ownerId: String, template: SchoolTemplate): Result<Unit>
}
