package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuratime.features.account.data.local.AccountClassAccessDao
import com.azuratech.azuratime.features.account.domain.model.SupervisorAssignment
import com.azuratech.azuratime.features.account.domain.model.toDomain
import com.azuratech.azuratime.features.account.domain.model.toEntity
import com.azuratech.azuratime.features.account.domain.repository.AccountClassAccessRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 🎓 ACCOUNT CLASS ACCESS REPOSITORY IMPLEMENTATION (v3.8.0)
 * Concrete repository implementation managing AccountClassAccess Room operations.
 */
class AccountClassAccessRepositoryImpl @Inject constructor(
    private val dao: AccountClassAccessDao,
) : AccountClassAccessRepository {

    override fun observeAssignments(accountId: String, schoolId: String): Flow<List<SupervisorAssignment>> {
        return dao.getAssignmentsEntityFlow(accountId, schoolId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateAssignments(accountId: String, schoolId: String, assignments: List<SupervisorAssignment>) {
        val entities = assignments.map { it.toEntity() }
        dao.replaceAssignments(accountId, schoolId, entities)
    }
}
