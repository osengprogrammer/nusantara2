package com.azuratech.azuratime.features.payment.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.payment.data.local.PaymentEntity
import com.azuratech.azuratime.features.payment.ui.history.PaymentHistoryUiEffect
import com.azuratech.azuratime.features.payment.ui.history.PaymentHistoryViewModel
import com.azuratech.azuratime.core.ui.components.StudentRosterItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaymentHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showActionDialog by remember { mutableStateOf<PaymentAction?>(null) }

    LaunchedEffect(viewModel.uiEffectFlow) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is PaymentHistoryUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isStudentSelected = uiState.selectedStudentId != null

    AzuraScreen(
        title = if (isStudentSelected) "Wallet Details" else "Student Wallets",
        onBack = {
            if (isStudentSelected) {
                // De-select student to return to roster
                viewModel.selectStudent(null)
            } else {
                onNavigateBack()
            }
        },
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (isStudentSelected) {
            StudentWalletDetailsContent(
                studentName = uiState.selectedStudentName ?: "",
                studentCode = uiState.selectedStudentCode,
                balance = uiState.selectedStudentBalance,
                payments = uiState.payments,
                onTopUpClick = { showActionDialog = PaymentAction.TOP_UP },
                onDeductClick = { showActionDialog = PaymentAction.DEDUCT },
            )
        } else {
            StudentRosterContent(
                students = uiState.students,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onStudentClick = { student -> viewModel.selectStudent(student.studentId) },
            )
        }
    }

    // Action dialogs for Top-Up / Deduction
    showActionDialog?.let { action ->
        val title = if (action == PaymentAction.TOP_UP) "Top Up Balance" else "Deduct Balance"
        val buttonText = if (action == PaymentAction.TOP_UP) "Add Funds" else "Deduct Funds"
        var amountText by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showActionDialog = null },
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter amount to proceed for ${uiState.selectedStudentName ?: "Student"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            isError = it.toDoubleOrNull() == null || it.toDouble() <= 0
                        },
                        label = { Text("Amount (Rp)") },
                        singleLine = true,
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isError) {
                        Text(
                            text = "Please enter a valid positive number",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val studentId = uiState.selectedStudentId ?: return@Button
                            if (action == PaymentAction.TOP_UP) {
                                viewModel.topUp(studentId, amount)
                            } else {
                                viewModel.deduct(studentId, amount)
                            }
                            showActionDialog = null
                        } else {
                            isError = true
                        }
                    },
                    enabled = !uiState.isPerformingAction,
                ) {
                    Text(buttonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

enum class PaymentAction {
    TOP_UP, DEDUCT
}

@Composable
fun StudentRosterContent(
    students: List<StudentRosterItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStudentClick: (StudentRosterItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name or code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AzuraSpacing.sm),
            singleLine = true,
        )

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No students found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AzuraSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                items(students, key = { it.studentId }) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStudentClick(student) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AzuraSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Class: ${student.assignedClassNames}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!student.studentCode.isNullOrBlank()) {
                                    Text(
                                        text = "Code: ${student.studentCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = student.formattedBalance(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentWalletDetailsContent(
    studentName: String,
    studentCode: String?,
    balance: Double,
    payments: List<PaymentEntity>,
    onTopUpClick: () -> Unit,
    onDeductClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AzuraSpacing.sm),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AzuraSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = studentName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (!studentCode.isNullOrBlank()) {
                    Text(
                        text = "Student Code: $studentCode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Current Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = "Rp %.0f".format(balance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                ) {
                    Button(
                        onClick = onTopUpClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Top Up")
                    }

                    Button(
                        onClick = onDeductClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deduct")
                    }
                }
            }
        }

        Text(
            text = "Transaction History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = AzuraSpacing.sm),
        )

        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
            ) {
                items(payments, key = { it.id }) { payment ->
                    TransactionItem(payment = payment)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(payment: PaymentEntity) {
    val isTopUp = payment.type == "TOP_UP"
    val color = if (isTopUp) Color(0xFF2E7D32) else Color(0xFFC62828)
    val prefix = if (isTopUp) "+" else "-"

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(payment.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AzuraSpacing.md, vertical = AzuraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTopUp) "Top Up" else "Deduction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (payment.performedByAccountName.isNotBlank()) {
                    Text(
                        text = "By: ${payment.performedByAccountName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "$prefix Rp %.0f".format(payment.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}
