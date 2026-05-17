with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "r") as f:
    content = f.read()

# fix the orphaned } at line 54
content = content.replace("        is UiState.Success -> {\n\n            }\n              \n            DashboardContent(", "        is UiState.Success -> {\n            DashboardContent(")

import re
content = re.sub(r"is UiState\.Success -> \{\s*\}\s*DashboardContent\(", "is UiState.Success -> {\n            DashboardContent(", content)
content = re.sub(r"is UiState\.Success -> \{\s*DashboardContent\(", "is UiState.Success -> {\n            if (state.data.conflicts.isNotEmpty()) {\n                com.azuratech.azuratime.ui.components.ConflictResolverDialog(\n                    conflict = state.data.conflicts.first(),\n                    onResolve = { useCloud -> viewModel.resolveConflict(state.data.conflicts.first(), useCloud) }\n                )\n            }\n            DashboardContent(", content)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "w") as f:
    f.write(content)
