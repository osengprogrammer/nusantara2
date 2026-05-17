import os
import re

# Collect all renamed variables from the previous run
# Or just run a global replacement for common ViewModel properties
replacements = {
    r'\buiState\b': 'uiStateFlow',
    r'\b_uiState\b': '_uiStateFlow',
    r'\bsideEffect\b': 'sideEffectFlow',
    r'\b_sideEffect\b': '_sideEffectFlow',
    r'\bsideEffectFlowFlow\b': 'sideEffectFlow', # Fix double suffix if any
    r'\buiStateFlowFlow\b': 'uiStateFlow',
    r'\bauthState\b': 'authStateFlow',
    r'\b_authState\b': '_authStateFlow',
    r'\bauthStateFlowFlow\b': 'authStateFlow',
}

# I should also handle specific ones like 'classes', 'accountsList', etc.
# But for now, let's focus on the common ones.

def apply_global_renames(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements.items():
        new_content = re.sub(old, new, new_content)
    
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            apply_global_renames(os.path.join(root, file))
