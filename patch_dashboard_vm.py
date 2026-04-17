with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "import com.azuratech.azuratime.data.repository.UserRepository" in line:
        new_lines.append(line)
        new_lines.append("import com.azuratech.azuratime.data.repository.AdminRepository\n")
        new_lines.append("import kotlinx.coroutines.channels.Channel\n")
    elif "private val userRepository: UserRepository," in line:
        new_lines.append("    private val adminRepository: AdminRepository,\n")
        new_lines.append(line)
    elif "private val syncViewModel: SyncViewModel" in line:
        new_lines.append(line)
        new_lines.append("\n    private val _syncCompletedEvent = Channel<Unit>()\n")
        new_lines.append("    val syncCompletedEvent = _syncCompletedEvent.receiveAsFlow()\n")
        new_lines.append("\n    init {\n")
        new_lines.append("        viewModelScope.launch {\n")
        new_lines.append("            _userFlow.collectLatest { user ->\n")
        new_lines.append("                if (user != null) {\n")
        new_lines.append("                    checkInRepository.setActiveClass(user.activeClassId)\n")
        new_lines.append("                    val role = user.activeSchoolId?.let { user.memberships[it]?.role }\n")
        new_lines.append("                    if (role == \"ADMIN\" && user.activeSchoolId != null) {\n")
        new_lines.append("                        adminRepository.startObservingTeachers(user.activeSchoolId)\n")
        new_lines.append("                    }\n")
        new_lines.append("                }\n")
        new_lines.append("            }\n")
        new_lines.append("        }\n")
        new_lines.append("    }\n")
    elif "dataIntegrityRepository.globalUnsyncedCount" in line:
        new_lines.append("        dataIntegrityRepository.globalUnsyncedCount,\n")
        new_lines.append("        userRepository.conflicts\n")
    elif "val unsynced = args[8] as Int" in line:
        new_lines.append(line)
        new_lines.append("        @Suppress(\"UNCHECKED_CAST\")\n")
        new_lines.append("        val conflicts = args[9] as List<com.azuratech.azuratime.data.local.AttendanceConflict>\n")
    elif "unsyncedRecords = unsynced" in line:
        new_lines.append("            unsyncedRecords = unsynced,\n")
        new_lines.append("            conflicts = conflicts\n")
    elif "fun sync() {" in line:
        new_lines.append(line)
        new_lines.append("        syncViewModel.forceSyncFromCloud {\n")
        new_lines.append("            viewModelScope.launch { _syncCompletedEvent.send(Unit) }\n")
        new_lines.append("        }\n")
    elif "syncViewModel.forceSyncFromCloud {}" in line:
        pass
    else:
        new_lines.append(line)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "w") as f:
    f.writelines(new_lines)
    
