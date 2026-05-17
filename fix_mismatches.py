import os
import re

def fix_mismatches(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    
    # Common pairs to fix
    pairs = [
        ('_state', '_stateFlow'),
        ('_uiState', '_uiStateFlow'),
        ('_uiEvent', '_uiEventFlow'),
        ('_sideEffect', '_sideEffectFlow'),
        ('_searchQuery', '_searchQueryFlow'),
        ('_refreshTrigger', '_refreshTriggerFlow'),
        ('_allClasses', '_allClassesFlow'),
        ('_accountsList', '_accountsListFlow'),
        ('_zoharResponse', '_zoharResponseFlow'),
        ('_isLoading', '_isLoadingFlow'),
        ('_enrollmentList', '_enrollmentListFlow'),
        ('_pendingSchools', '_pendingSchoolsFlow')
    ]
    
    modified = False
    for old, new in pairs:
        # If the file uses the 'new' name but only declares the 'old' one
        if new in content and old in content:
            # Check if 'old' is declared but 'new' is not
            is_old_declared = re.search(r'\bval\s+' + old + r'\b', content) or re.search(r'\bvar\s+' + old + r'\b', content)
            is_new_declared = re.search(r'\bval\s+' + new + r'\b', content) or re.search(r'\bvar\s+' + new + r'\b', content)
            
            if is_old_declared and not is_new_declared:
                # Rename the declaration and all instances of the old name to the new name
                new_content = re.sub(r'\b' + old + r'\b', new, new_content)
                modified = True

    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

# Files identified with errors
error_files = [
    'app/src/main/java/com/azuratech/azuratime/core/boot/BootViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/account/ui/management/AccountManagementViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/account/ui/components/AdminViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/account/ui/components/NetworkViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/account/ui/components/WorkspaceViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/biometric/ui/enroll/BiometricEnrollmentViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/reporting/ui/ReportViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/reporting/ui/matrix/AttendanceMatrixViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/school/ui/classes/ClassViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/school/ui/list/SchoolViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/student/ui/bulk/RegisterViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/student/ui/form/StudentFormViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/student/ui/roster/StudentRosterViewModel.kt',
    'app/src/main/java/com/azuratech/azuratime/features/dashboard/ui/DashboardViewModel.kt'
]

for file_path in error_files:
    if os.path.exists(file_path):
        if fix_mismatches(file_path):
            print(f"Fixed mismatches in {file_path}")
