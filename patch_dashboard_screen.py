import re

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "r") as f:
    content = f.read()

# Add LaunchedEffect and Toast
content = content.replace(
    "val context = LocalContext.current\n",
    "val context = LocalContext.current\n\n    LaunchedEffect(Unit) {\n        viewModel.syncCompletedEvent.collect {\n            android.widget.Toast.makeText(context, \"Sinkronisasi Selesai!\", android.widget.Toast.LENGTH_SHORT).show()\n        }\n    }\n"
)

# Show ConflictResolverDialog
dialog_code = """
            if (state.data.conflicts.isNotEmpty()) {
                com.azuratech.azuratime.ui.components.ConflictResolverDialog(
                    conflict = state.data.conflicts.first(),
                    onResolve = { useCloud -> viewModel.resolveConflict(state.data.conflicts.first(), useCloud) }
                )
            }
            
            DashboardContent(
"""
content = content.replace("            DashboardContent(\n", dialog_code)

# Replace Backup Button with DashboardSyncButton
old_actions = """        actions = {
            IconButton(onClick = {
                com.azuratech.azuratime.domain.sync.BackupUtils.backupAndShareDatabase(navController.context)
            }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Share,
                    contentDescription = "Backup Database"
                )
            }
            WorkspaceSelector(
                currentUser = data.user,
                workspaceViewModel = hiltViewModel()
            )
        }"""

new_actions = """        actions = {
            WorkspaceSelector(
                currentUser = data.user,
                workspaceViewModel = hiltViewModel()
            )
            DashboardSyncButton(
                isSyncing = data.isSyncing,
                onSyncClick = onSyncClick
            )
        }"""

content = content.replace(old_actions, new_actions)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "w") as f:
    f.write(content)
