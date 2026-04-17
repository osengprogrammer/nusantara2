with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "r") as f:
    content = f.read()

# remove lines 40-43
import re
content = re.sub(r"    40                  \}\n    41              \}\n    42          \}\n    43      \}\n", "", content) # not good, just remove the specific lines

content = content.replace("                }\n            }\n        }\n    }\n\n    private val _userFlow", "    private val _userFlow")

init_code = """
    init {
        viewModelScope.launch {
            _userFlow.collectLatest { user ->
                if (user != null) {
                    checkInRepository.setActiveClass(user.activeClassId)
                    val role = user.activeSchoolId?.let { user.memberships[it]?.role }
                    if (role == "ADMIN" && user.activeSchoolId != null) {
                        adminRepository.startObservingTeachers(user.activeSchoolId)
                    }
                }
            }
        }
    }
"""

content = content.replace("    val state: StateFlow<UiState<DashboardUiState>>", init_code + "\n    val state: StateFlow<UiState<DashboardUiState>>")

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "w") as f:
    f.write(content)
