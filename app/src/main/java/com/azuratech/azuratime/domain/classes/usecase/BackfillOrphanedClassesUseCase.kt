package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repo.SchoolRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UseCase to backfill classes that have no schoolId assigned.
 */
class BackfillOrphanedClassesUseCase @Inject constructor(
    private val repository: SchoolRepository,
    private val sessionManager: SessionManager
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val resolvedId = sessionManager.getActiveSchoolId() ?: return@withContext
        val orphaned = repository.getOrphanedClasses()
        
        if (orphaned.isNotEmpty()) {
            println("🔧 Backfill: Found ${orphaned.size} orphaned classes")
            orphaned.forEach { cls ->
                repository.updateClassSchool(cls.id, resolvedId)
                println("✅ Backfill: Updated class '${cls.name}' → schoolId=$resolvedId")
            }
        }
    }
}
