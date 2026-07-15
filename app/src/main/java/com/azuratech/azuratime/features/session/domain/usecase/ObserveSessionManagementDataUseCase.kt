package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.core.data.local.SessionWithDetails
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.session.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

/**
 * 🔒 OBSERVE SESSION MANAGEMENT DATA USE CASE
 * Combines all repository observation flows needed by SessionManagementViewModel
 * into a single reactive stream. Keeps the ViewModel free of direct repository dependencies.
 */
class ObserveSessionManagementDataUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val schoolRepository: SchoolRepository,
    private val accountRepository: AccountRepository,
) {
    data class SessionManagementData(
        val subjectsResult: Result<List<SubjectEntity>>,
        val sessionsResult: Result<List<SessionWithDetails>>,
        val classesResult: Result<List<ClassModel>>,
        val accountResult: Result<Account>,
        val schoolId: String,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        schoolIdFlow: Flow<String?>,
        accountIdFlow: Flow<String?>,
    ): Flow<SessionManagementData> =
        combine(schoolIdFlow.filterNotNull(), accountIdFlow.filterNotNull()) { schoolId, accountId ->
            schoolId to accountId
        }.flatMapLatest { (schoolId, accountId) ->
            combine(
                sessionRepository.observeAllSubjectsFlow(schoolId),
                sessionRepository.observeAllSessionsFlow(schoolId),
                schoolRepository.observeClassesFlow(schoolId),
                accountRepository.getAccountFlow(accountId),
            ) { subjectsResult, sessionsResult, classesResult, accountResult ->
                SessionManagementData(subjectsResult, sessionsResult, classesResult, accountResult, schoolId)
            }
        }
}
