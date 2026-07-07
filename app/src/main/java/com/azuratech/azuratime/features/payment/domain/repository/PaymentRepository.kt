package com.azuratech.azuratime.features.payment.domain.repository

import com.azuratech.azuratime.features.payment.data.local.PaymentEntity
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPaymentsByStudentFlow(studentId: String): Flow<List<PaymentEntity>>
    suspend fun topUpBalance(studentId: String, schoolId: String, amount: Double, performedByAccountId: String, performedByAccountName: String)
    suspend fun deductBalance(studentId: String, schoolId: String, amount: Double, performedByAccountId: String, performedByAccountName: String)
}
