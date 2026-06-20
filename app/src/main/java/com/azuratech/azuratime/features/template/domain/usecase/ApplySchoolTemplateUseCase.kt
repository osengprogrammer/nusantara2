package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.school.data.local.ClassEntity
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * 🚀 APPLY SCHOOL TEMPLATE USE CASE (v1.0.0)
 * Orchestrates applying a selected global school template to the active school.
 * Fetches template classes/subjects, maps them into the active school workspace as unsynced entities,
 * persists them atomically, and triggers an immediate synchronization.
 */
class ApplySchoolTemplateUseCase @Inject constructor(
    private val repository: TemplateRepository,
    private val syncManager: SyncManager,
) {
    suspend operator fun invoke(
        schoolId: String,
        ownerId: String,
        template: SchoolTemplate,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. Fetch Class Templates in Batch
        val classesResult = repository.fetchGlobalClassesByIds(template.defaultClassIds)

        classesResult.flatMap { classTemplates ->
            // 2. Fetch Subject Templates in Batch
            val subjectsResult = repository.fetchGlobalSubjectsByIds(template.defaultSubjectIds)

            subjectsResult.flatMap { subjectTemplates ->
                // 3. Map Templates to local Class and Subject Entities (with isSynced = false)
                val classEntities = classTemplates.map { classTemplate ->
                    ClassEntity(
                        id = UUID.randomUUID().toString(),
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
                        subjectId = UUID.randomUUID().toString(),
                        name = subjectTemplate.name,
                        description = null,
                        schoolId = schoolId,
                        isActive = true,
                        isSynced = false,
                        isFromTemplate = true,
                    )
                }

                // 4. Atomic persist to Room database (wrapped in withTransaction in TemplateRepository)
                val persistResult = repository.persistTemplateData(classEntities, subjectEntities)

                persistResult.flatMap {
                    // 5. Trigger Immediate Background Synchronization
                    syncManager.enqueueSync()
                    Result.Success(Unit)
                }
            }
        }
    }
}
