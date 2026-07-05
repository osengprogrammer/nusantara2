package com.azuratech.azuratime.features.account.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.features.school.ui.classes.ClassViewModel
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.account.ui.management.AccountUiEvent
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun MyAssignedClassScreen(
    onNavigateBack: () -> Unit,
    accountViewModel: AccountManagementViewModel,
    classViewModel: ClassViewModel,
    targetAccountId: String? = null,
) {
    val assignedIds by (
        if (targetAccountId == null) {
            accountViewModel.assignedClassIdsFlow
        } else {
            accountViewModel.targetAssignedClassIdsFlow
        }
        )
        .collectAsStateWithLifecycle()
    val classUiState by classViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val allClasses = classUiState.classes
    val account by accountViewModel.currentAccountFlow.collectAsStateWithLifecycle()
    val targetAccount by accountViewModel.selectedTargetAccountFlow.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val myClasses = remember(allClasses, assignedIds, searchQuery) {
        allClasses.filter { classItem ->
            classItem.id in assignedIds && classItem.name.contains(searchQuery, true)
        }
    }

    val availableClasses = remember(allClasses, assignedIds, searchQuery) {
        allClasses.filter { classItem ->
            classItem.id !in assignedIds && classItem.name.contains(searchQuery, true)
        }
    }

    val screenTitle = if (targetAccountId == null) {
        stringResource(id = R.string.title_my_assigned_classes)
    } else {
        stringResource(
            id = R.string.title_assigned_classes_for,
            targetAccount?.name ?: targetAccountId,
        )
    }

    LaunchedEffect(account?. activeClassId) {
        println("✅ DEBUG: UI received updated activeClassId=${account?. activeClassId}")
    }

    MyAssignedClassContent(
        title = screenTitle,
        myClasses = myClasses,
        availableClasses = availableClasses,
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        onRemoveClass = { classId -> accountViewModel.onEvent(AccountUiEvent.RemoveClassAccess(classId, targetAccountId)) },
        onSelectActiveClass = { classId ->
            println("🖱 DEBUG: Pilih Sesi clicked for classId=$classId")
            accountViewModel.onEvent(AccountUiEvent.SelectActiveClass(classId, targetAccountId))
        },
        onAssignClass = { classId -> accountViewModel.onEvent(AccountUiEvent.AssignClassToAccount(classId, targetAccountId)) },
        account = account,
        onBack = onNavigateBack,
    )
}

@Composable
fun MyAssignedClassContent(
    title: String,
    myClasses: List<ClassModel>,
    availableClasses: List<ClassModel>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRemoveClass: (String) -> Unit,
    onSelectActiveClass: (String) -> Unit,
    onAssignClass: (String) -> Unit,
    account: com.azuratech.azuratime.features.account.domain.model.AccountProfile?,
    onBack: () -> Unit,
) {
    AzuraScreen(
        title = title,
        onBack = onBack,
        content = {
            Column(modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.md)) {
                AzuraTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = stringResource(id = R.string.search_classes),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                )

                Spacer(Modifier.height(AzuraSpacing.md))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(id = R.string.header_held_classes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    if (myClasses.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.empty_held_classes),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else {
                        items(myClasses, key = { it.id }) { classItem ->
                            val isActive = account?. activeClassId == classItem.id

                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                ),
                                content = {
                                    Row(
                                        modifier = Modifier.padding(0.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(classItem.name, fontWeight = FontWeight.Bold)
                                            Text("ID: ${classItem.id}", style = MaterialTheme.typography.labelSmall)
                                        }

                                        IconButton(onClick = { onRemoveClass(classItem.id) }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = stringResource(id = R.string.desc_remove_action), tint = MaterialTheme.colorScheme.error)
                                        }

                                        if (account != null) {
                                            if (isActive) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(id = R.string.desc_active_status), tint = Color(0xFF00C853))
                                            } else {
                                                AzuraButton(
                                                    text = stringResource(id = R.string.action_select_session_btn),
                                                    onClick = {
                                                        println("🚨 HARD LOG: Button Clicked for ${classItem.id}")
                                                        onSelectActiveClass(classItem.id)
                                                    },
                                                    modifier = Modifier.height(32.dp),
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }

                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.md))
                    }

                    item {
                        Text(
                            text = stringResource(id = R.string.header_add_class_authority),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }

                    if (availableClasses.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(id = R.string.empty_available_classes),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    } else {
                        items(availableClasses, key = { it.id }) { classItem ->
                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                                content = {
                                    Row(
                                        modifier = Modifier.padding(0.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Gray)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(classItem.name, fontWeight = FontWeight.Medium)
                                        }

                                        IconButton(onClick = { onAssignClass(classItem.id) }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(id = R.string.desc_add_action), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
