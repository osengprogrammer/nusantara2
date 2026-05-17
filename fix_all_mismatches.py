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
        # If the file uses the 'new' name but ONLY declares the 'old' one
        if new in content and old in content:
            # Check if 'old' is declared as a val/var but 'new' is NOT declared as a val/var
            is_old_declared = re.search(r'\b(?:val|var)\s+' + old + r'\b', content)
            is_new_declared = re.search(r'\b(?:val|var)\s+' + new + r'\b', content)
            
            if is_old_declared and not is_new_declared:
                # Rename all occurrences of 'old' to 'new' (surgical word boundary replacement)
                new_content = re.sub(r'\b' + old + r'\b', new, new_content)
                modified = True

    if modified:
        # Final cleanup: avoid triple Flow suffixes if any
        new_content = new_content.replace('FlowFlowFlow', 'Flow')
        new_content = new_content.replace('FlowFlow', 'Flow')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

# Scan ALL Kotlin files in the project
for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            if fix_mismatches(path):
                print(f"Fixed mismatches in {path}")
