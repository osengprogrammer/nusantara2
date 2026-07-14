package com.azuratech.azuratime.features.payment.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.data.local.StudentWalletDao
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.payment.domain.repository.PaymentRepository
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.core.data.local.toDomain
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.core.ui.components.StudentRosterItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val walletDao: StudentWalletDao,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val initialStudentId: String = savedStateHandle.get<String>("studentId") ?: ""

    private val _uiStateFlow = MutableStateFlow(PaymentHistoryUiState())
    val uiStateFlow: StateFlow<PaymentHistoryUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<PaymentHistoryUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedStudentIdFlow = MutableStateFlow<String?>(initialStudentId.ifBlank { null })

    private var historyJob: Job? = null

    // Observe active school
    private val _activeSchoolIdFlow = sessionManager.activeSchoolIdFlow

    // Observe classes for roster mapping
    private val _allClassesFlow = _activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClassesFlow(schoolId).map { result ->
                result.getOrNull() ?: emptyList()
            }
        }

    // Observe all wallets for school
    private val _walletsFlow = _activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            walletDao.getAllWalletsBySchool(schoolId)
        }

    // Current logged in account to retrieve performedBy credentials
    private val _currentAccountFlow: StateFlow<Account?> = sessionManager.currentAccountIdFlow
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.observeAccountEntityFlow(accountId).map { result -> result.getOrNull()?.toDomain() }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    init {
        // Automatically select the student if studentId is passed in SavedStateHandle
        if (initialStudentId.isNotBlank()) {
            selectStudent(initialStudentId)
        } else {
            observeStudentRoster()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQueryFlow.value = query
        _uiStateFlow.update { it.copy(searchQuery = query) }
    }

    fun selectStudent(studentId: String?) {
        _selectedStudentIdFlow.value = studentId
        historyJob?.cancel()

        if (studentId == null) {
            _uiStateFlow.update {
                it.copy(
                    selectedStudentId = null,
                    selectedStudentName = null,
                    selectedStudentCode = null,
                    selectedStudentBalance = 0.0,
                    payments = emptyList(),
                )
            }
            observeStudentRoster()
            return
        }

        // Fetch details of selected student and subscribe to their transaction history
        historyJob = viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }

            // Retrieve selected student profile details
            val profileFlow = studentRepository.getStudentProfilesFlow().map { result ->
                result.getOrNull()?.find { it.studentId == studentId }
            }

            // Retrieve selected student wallet balance
            val balanceFlow = walletDao.getBalanceFlow(studentId)

            // Retrieve transaction history
            val paymentsFlow = paymentRepository.getPaymentsByStudentFlow(studentId)

            combine(profileFlow, balanceFlow, paymentsFlow) { profile, balance, payments ->
                Triple(profile, balance, payments)
            }.catch { e ->
                _uiStateFlow.update { it.copy(isLoading = false) }
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Failed: ${e.message}"))
            }.collect { (profile, balance, payments) ->
                _uiStateFlow.update {
                    it.copy(
                        isLoading = false,
                        selectedStudentId = studentId,
                        selectedStudentName = profile?.name ?: "Unknown Student",
                        selectedStudentCode = profile?.studentCode,
                        selectedStudentBalance = balance ?: 0.0,
                        payments = payments,
                    )
                }
            }
        }
    }

    private fun observeStudentRoster() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            combine(
                studentRepository.getStudentProfilesFlow(),
                _allClassesFlow,
                _searchQueryFlow,
                _walletsFlow,
            ) { profilesResult, classes, query, wallets ->
                val profiles = profilesResult.getOrNull() ?: emptyList()
                val classMap = classes.associateBy { it.id }

                profiles
                    .filter { profile ->
                        profile.name.contains(query, ignoreCase = true) ||
                            (profile.studentCode?.contains(query, ignoreCase = true) ?: false)
                    }
                    .map { profile ->
                        val assignedClassNames = profile.classIds
                            .mapNotNull { classMap[it]?.name }
                            .joinToString(", ")
                            .ifEmpty { "No Class" }

                        val wallet = wallets.find { it.studentId == profile.studentId }
                        StudentRosterItem(
                            studentId = profile.studentId,
                            displayName = profile.name,
                            studentCode = profile.studentCode,
                            assignedClassNames = assignedClassNames,
                            isBiometricReady = profile.embedding != null,
                            currentBalance = wallet?.currentBalance ?: 0.0,
                        )
                    }
            }.catch { e ->
                _uiStateFlow.update { it.copy(isLoading = false) }
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Roster load error: ${e.message}"))
            }.collect { rosterItems ->
                _uiStateFlow.update {
                    it.copy(
                        isLoading = false,
                        students = rosterItems,
                    )
                }
            }
        }
    }

    fun topUp(studentId: String, amount: Double) {
        val schoolId = _activeSchoolIdFlow.value ?: return
        val performer = _currentAccountFlow.value
        val performerId = performer?.accountId ?: "unknown_admin"
        val performerName = performer?.name ?: "Admin/Supervisor"

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isPerformingAction = true) }
            try {
                paymentRepository.topUpBalance(
                    studentId = studentId,
                    schoolId = schoolId,
                    amount = amount,
                    performedByAccountId = performerId,
                    performedByAccountName = performerName,
                )
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Successfully topped up Rp %.0f".format(amount)))
            } catch (e: Exception) {
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Top-up failed: ${e.message}"))
            } finally {
                _uiStateFlow.update { it.copy(isPerformingAction = false) }
            }
        }
    }

    fun deduct(studentId: String, amount: Double) {
        val schoolId = _activeSchoolIdFlow.value ?: return
        val performer = _currentAccountFlow.value
        val performerId = performer?.accountId ?: "unknown_admin"
        val performerName = performer?.name ?: "Admin/Supervisor"

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isPerformingAction = true) }
            try {
                paymentRepository.deductBalance(
                    studentId = studentId,
                    schoolId = schoolId,
                    amount = amount,
                    performedByAccountId = performerId,
                    performedByAccountName = performerName,
                )
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Successfully deducted Rp %.0f".format(amount)))
            } catch (e: Exception) {
                _uiEffectFlow.emit(PaymentHistoryUiEffect.ShowToast("Deduction failed: ${e.message}"))
            } finally {
                _uiStateFlow.update { it.copy(isPerformingAction = false) }
            }
        }
    }
}
