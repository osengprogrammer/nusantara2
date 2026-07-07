package com.azuratech.azuratime.features.payment.domain.repository

import com.azuratech.azuratime.core.data.local.StudentWalletDao
import com.azuratech.azuratime.core.data.local.StudentWalletEntity
import com.azuratech.azuratime.features.payment.data.local.PaymentDao
import com.azuratech.azuratime.features.payment.data.local.PaymentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val walletDao: StudentWalletDao
) : PaymentRepository {

    override fun getPaymentsByStudentFlow(studentId: String): Flow<List<PaymentEntity>> {
        return paymentDao.getPaymentsByStudent(studentId)
    }

    override suspend fun topUpBalance(
        studentId: String, 
        schoolId: String, 
        amount: Double, 
        performedByAccountId: String, 
        performedByAccountName: String
    ) {
        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            schoolId = schoolId,
            amount = amount,
            type = "TOP_UP",
            timestamp = System.currentTimeMillis(),
            performedByAccountId = performedByAccountId,
            performedByAccountName = performedByAccountName,
            isSynced = false
        )
        paymentDao.insertPayment(payment)

        val currentBalance = walletDao.getBalanceFlow(studentId).firstOrNull() ?: 0.0
        walletDao.upsertWallet(
            StudentWalletEntity(
                studentId = studentId,
                schoolId = schoolId,
                currentBalance = currentBalance + amount,
                isSynced = false
            )
        )
    }

    override suspend fun deductBalance(
        studentId: String, 
        schoolId: String, 
        amount: Double, 
        performedByAccountId: String, 
        performedByAccountName: String
    ) {
        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            schoolId = schoolId,
            amount = amount,
            type = "DEDUCTION",
            timestamp = System.currentTimeMillis(),
            performedByAccountId = performedByAccountId,
            performedByAccountName = performedByAccountName,
            isSynced = false
        )
        paymentDao.insertPayment(payment)

        val currentBalance = walletDao.getBalanceFlow(studentId).firstOrNull() ?: 0.0
        walletDao.upsertWallet(
            StudentWalletEntity(
                studentId = studentId,
                schoolId = schoolId,
                currentBalance = currentBalance - amount,
                isSynced = false
            )
        )
    }
}
