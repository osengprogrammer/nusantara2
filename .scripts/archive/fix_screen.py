with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "r") as f:
    content = f.read()

# Just replace `DashboardContent(` with the dialog ONLY in the main screen where `state.data` is valid!
# We can find `is UiState.Success -> {\n\n            DashboardContent(`
dialog_code = """is UiState.Success -> {

            if (state.data.conflicts.isNotEmpty()) {
                com.azuratech.azuratime.ui.components.ConflictResolverDialog(
                    conflict = state.data.conflicts.first(),
                    onResolve = { useCloud -> viewModel.resolveConflict(state.data.conflicts.first(), useCloud) }
                )
            }
            
            DashboardContent("""

content = content.replace("is UiState.Success -> {\n\n            DashboardContent(", dialog_code)
content = content.replace("is UiState.Success -> {\n            DashboardContent(", dialog_code)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "w") as f:
    f.write(content)
