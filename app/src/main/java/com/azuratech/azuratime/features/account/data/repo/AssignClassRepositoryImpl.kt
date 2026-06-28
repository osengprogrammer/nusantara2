package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuratime.features.account.data.local.AccountClassAccessDao
import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.data.local.toDomain
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.model.TeacherAssignment
import com.azuratech.azuratime.features.account.domain.repository.AssignClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 🎓 ASSIGN CLASS REPOSITORY IMPLEMENTATION (v3.9.0)
 * Concrete repository implementation managing Account and AccountClassAccess Room operations.
 */
class AssignClassRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val accountClassAccessDao: AccountClassAccessDao,
) : AssignClassRepository {

    override suspend fun getAccountById(id: String): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override fun observeAssignments(accountId: String, schoolId: String): Flow<List<TeacherAssignment>> {
        return accountClassAccessDao.getAssignmentsFlow(accountId, schoolId).map { tuples ->
            tuples.map { tuple ->
                TeacherAssignment(
                    classId = tuple.classId,
                    subjectId = tuple.subjectId.takeIf { it.isNotEmpty() },
                )
            }
        }
    }
}
