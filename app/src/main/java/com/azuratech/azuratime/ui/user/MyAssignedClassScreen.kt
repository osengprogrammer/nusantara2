package com.azuratech.azuratime.ui.user

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun MyAssignedClassScreen(
    onNavigateBack: () -> Unit,
    userViewModel: UserManagementViewModel,
    classViewModel: ClassViewModel,
    targetUserId: String? = null
) {
    val assignedIds by (if (targetUserId == null) userViewModel.assignedClassIds
                        else userViewModel.targetAssignedClassIds)
                        .collectAsStateWithLifecycle(emptyList())
    val allClasses by classViewModel.classes.collectAsStateWithLifecycle(emptyList())
    val user by userViewModel.currentUser.collectAsStateWithLifecycle()
    val targetUser by userViewModel.selectedTargetUser.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val myClasses = remember(allClasses, assignedIds, searchQuery) {
        allClasses.filter { it.id in assignedIds && it.name.contains(searchQuery, true) }
    }

    val availableClasses = remember(allClasses, assignedIds, searchQuery) {
        allClasses.filter { it.id !in assignedIds && it.name.contains(searchQuery, true) }
    }

    val screenTitle = if (targetUserId == null) "Otoritas Kelas Saya"
                      else "Otoritas Kelas: ${targetUser?.name ?: targetUserId}"

    LaunchedEffect(user?.activeClassId) {
        println("✅ DEBUG: UI received updated activeClassId=${user?.activeClassId}")
    }

    MyAssignedClassContent(
        title = screenTitle,
        myClasses = myClasses,
        availableClasses = availableClasses,
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        onRemoveClass = { classId -> userViewModel.removeClassAccess(classId, targetUserId) },
        onSelectActiveClass = { classId -> 
            println("🖱 DEBUG: Pilih Sesi clicked for classId=$classId")
            userViewModel.selectActiveClass(classId, targetUserId) 
        },
        onAssignClass = { classId -> userViewModel.assignClassToUser(classId, targetUserId) },
        user = user
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
    user: com.azuratech.azuratime.data.local.UserEntity?
) {
    AzuraScreen(
        title = title,
        content = {
            Column(modifier = Modifier.fillMaxSize().padding(top = AzuraSpacing.md)) {
                AzuraTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = "Cari Kelas...",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                Spacer(Modifier.height(AzuraSpacing.md))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "Kelas yang Saya Pegang",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (myClasses.isEmpty()) {
                        item {
                            Text(
                                "Belum ada otoritas kelas.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        items(myClasses, key = { it.id }) { classItem ->
                            val isActive = user?.activeClassId == classItem.id

                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                content = {
                                    Row(
                                        modifier = Modifier.padding(0.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(classItem.name, fontWeight = FontWeight.Bold)
                                            Text("ID: ${classItem.id}", style = MaterialTheme.typography.labelSmall)
                                        }

                                        IconButton(onClick = { onRemoveClass(classItem.id) }) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                        }

                                        if (user != null) {
                                            if (isActive) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Aktif", tint = Color(0xFF00C853))
                                            } else {
                                                AzuraButton(
                                                    text = "Pilih Sesi",
                                                    onClick = { 
                                                        println("🚨 HARD LOG: Button Clicked for ${classItem.id}")
                                                        onSelectActiveClass(classItem.id) 
                                                    },
                                                    modifier = Modifier.height(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        HorizontalDivider(Modifier.padding(vertical = AzuraSpacing.md))
                    }

                    item {
                        Text(
                            text = "Tambah Otoritas Kelas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (availableClasses.isEmpty()) {
                        item {
                            Text(
                                "Semua kelas sudah Anda pegang.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        items(availableClasses, key = { it.id }) { classItem ->
                            AzuraCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                content = {
                                    Row(
                                        modifier = Modifier.padding(0.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Gray)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(classItem.name, fontWeight = FontWeight.Medium)
                                        }

                                        IconButton(onClick = { onAssignClass(classItem.id) }) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
