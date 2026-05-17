import os
import re

# Properties that are NOT Flows but were likely incorrectly suffixed
state_props = [
    'searchQuery', 'isLoading', 'error', 'students', 'allClasses', 'selectedClassId',
    'targetStudentId', 'isDeleteDialogVisible', 'userProfile', 'activeClassId',
    'availableClasses', 'isEditingProfile', 'pendingPhotoUri', 'assignedClassIds',
    'allUsersInSameSchool', 'selectedTargetUser', 'targetAssignedClassIds',
    'status', 'role', 'schoolName', 'memberships'
]

# ViewModel properties that ARE Flows
vm_props = [
    'uiState', 'sideEffect', 'searchQuery', 'schoolSearchResults', 'accessRequests',
    'memberships', 'isRevoked', 'state', 'authState', 'isSyncing', 'pendingSchools',
    'enrollmentList'
]

def fix_kt_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    
    # 1. Fix state property access: uiState.propFlow -> uiState.prop
    for prop in state_props:
        new_content = re.sub(r'\buiState\.' + prop + r'Flow\b', 'uiState.' + prop, new_content)
        # Also handle it.propFlow
        new_content = re.sub(r'\bit\.' + prop + r'Flow\b', 'it.' + prop, new_content)
        # Also account.propFlow
        new_content = re.sub(r'\baccount\.' + prop + r'Flow\b', 'account.' + prop, new_content)

    # 2. Fix ViewModel property access: viewModel.prop -> viewModel.propFlow
    # But ONLY for vm_props and if they don't already have Flow suffix
    for prop in vm_props:
        # Avoid double suffixing
        new_content = re.sub(r'\bviewModel\.' + prop + r'\b(?!\s*Flow)', 'viewModel.' + prop + 'Flow', new_content)
        # Also bootViewModel.prop, mainViewModel.prop, etc.
        new_content = re.sub(r'\b([a-zA-Z0-9]+ViewModel)\.' + prop + r'\b(?!\s*Flow)', r'\1.' + prop + 'Flow', new_content)

    # 3. Fix local state variable declaration in Composables
    # val uiStateFlow by viewModel.uiStateFlow -> val uiState by ...
    new_content = re.sub(r'val\s+uiStateFlow\s+by', 'val uiState by', new_content)
    
    # 4. Final double Flow cleanup
    new_content = new_content.replace('FlowFlow', 'Flow')

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            fix_kt_file(os.path.join(root, file))
