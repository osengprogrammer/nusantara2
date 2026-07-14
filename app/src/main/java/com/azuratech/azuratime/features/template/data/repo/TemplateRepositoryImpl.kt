package com.azuratech.azuratime.features.template.data.repo
import com.azuratech.azuratime.core.domain.model.toSubjectTemplate

import androidx.room.withTransaction
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.ClassEntity
import com.azuratech.azuratime.core.data.local.toClassEntity
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.core.data.local.toSubjectEntity
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.model.ClassTemplate
import com.azuratech.azuratime.core.domain.model.SubjectTemplate
import com.azuratech.azuratime.features.template.domain.model.toSchoolTemplate
import com.azuratech.azuratime.features.template.domain.model.toClassTemplate
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
) : TemplateRepository {

    private val classDao = database.schoolClassDao()
    private val sessionDao = database.sessionDao()

    override suspend fun fetchClassesByIds(schoolId: String, classIds: List<String>): Result<List<ClassEntity>> = withContext(Dispatchers.IO) {
        if (classIds.isEmpty()) return@withContext Result.Success(emptyList())

        try {
            val results = mutableListOf<ClassEntity>()
            // 🔥 AI Native: Firestore 'whereIn' limit is 10-30 IDs per request
            val chunks = classIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("schools").document(schoolId)
                    .collection("classes")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                results.addAll(snapshot.documents.mapNotNull { it.toClassEntity(schoolId) })
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun fetchSubjectsByIds(schoolId: String, subjectIds: List<String>): Result<List<SubjectEntity>> = withContext(Dispatchers.IO) {
        if (subjectIds.isEmpty()) return@withContext Result.Success(emptyList())

        try {
            val results = mutableListOf<SubjectEntity>()
            val chunks = subjectIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("schools").document(schoolId)
                    .collection("subjects")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                results.addAll(snapshot.documents.mapNotNull { it.toSubjectEntity(schoolId) })
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun persistTemplateData(classes: List<ClassEntity>, subjects: List<SubjectEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // 🔥 Atomic: All or nothing
                if (classes.isNotEmpty()) {
                    classDao.insertClassesIgnore(classes)
                }
                if (subjects.isNotEmpty()) {
                    sessionDao.insertSubjectsIgnore(subjects)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun fetchSchoolTemplates(): Result<List<SchoolTemplate>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("school_templates")
                .get()
                .await()

            val templates = snapshot.documents.mapNotNull { it.toSchoolTemplate() }
            Result.Success(templates)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun fetchGlobalClassesByIds(classIds: List<String>): Result<List<ClassTemplate>> = withContext(Dispatchers.IO) {
        if (classIds.isEmpty()) return@withContext Result.Success(emptyList())

        try {
            val results = mutableListOf<ClassTemplate>()
            val chunks = classIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("global_classes")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                results.addAll(snapshot.documents.mapNotNull { it.toClassTemplate() })
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun fetchGlobalSubjectsByIds(subjectIds: List<String>): Result<List<SubjectTemplate>> = withContext(Dispatchers.IO) {
        if (subjectIds.isEmpty()) return@withContext Result.Success(emptyList())

        try {
            val results = mutableListOf<SubjectTemplate>()
            val chunks = subjectIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("global_subjects")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                results.addAll(snapshot.documents.mapNotNull { it.toSubjectTemplate() })
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun fetchAllGlobalClasses(): Result<List<ClassTemplate>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("global_classes")
                .get()
                .await()
            val classes = snapshot.documents.mapNotNull { it.toClassTemplate() }
            Result.Success(classes)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun fetchAllGlobalSubjects(): Result<List<SubjectTemplate>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("global_subjects")
                .get()
                .await()
            val subjects = snapshot.documents.mapNotNull { it.toSubjectTemplate() }
            Result.Success(subjects)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun applyTemplate(
        schoolId: String,
        ownerId: String,
        template: SchoolTemplate,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Class Templates in Batch
            val classesResult = fetchGlobalClassesByIds(template.defaultClassIds)
            if (classesResult is Result.Failure) return@withContext classesResult

            // 2. Fetch Subject Templates in Batch
            val subjectsResult = fetchGlobalSubjectsByIds(template.defaultSubjectIds)
            if (subjectsResult is Result.Failure) return@withContext subjectsResult

            val classTemplates = (classesResult as Result.Success).data
            val subjectTemplates = (subjectsResult as Result.Success).data

            // 3. Map Templates to local Class and Subject Entities (with isSynced = false)
            val classEntities = classTemplates.map { classTemplate ->
                ClassEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    ownerAccountId = ownerId,
                    schoolId = schoolId,
                    name = classTemplate.name,
                    grade = classTemplate.level.toString(),
                    accountId = null,
                    studentCount = 0,
                    createdAt = System.currentTimeMillis(),
                    isSynced = false,
                    isFromTemplate = true,
                )
            }

            val subjectEntities = subjectTemplates.map { subjectTemplate ->
                SubjectEntity(
                    subjectId = java.util.UUID.randomUUID().toString(),
                    name = subjectTemplate.name,
                    description = null,
                    schoolId = schoolId,
                    isActive = true,
                    isSynced = false,
                    isFromTemplate = true,
                )
            }

            // 4. Atomic persist to Room database
            persistTemplateData(classEntities, subjectEntities)
        } catch (e: Exception) {
            Result.Failure(com.azuratech.azuraengine.result.AppError.Network(e.message))
        }
    }
}
