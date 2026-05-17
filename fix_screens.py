import os
import re

def fix_screen_variable_names(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Fix local uiStateFlow to uiState in collectAsState
    # val uiStateFlow by viewModel.uiStateFlow.collectAsState... -> val uiState by ...
    new_content = re.sub(r'val\s+uiStateFlow\s+by', 'val uiState by', content)
    
    # 2. Fix parameter passing
    # uiState = uiStateFlow -> uiState = uiState
    new_content = re.sub(r'uiState\s*=\s*uiStateFlow', 'uiState = uiState', new_content)
    
    # 3. Fix preview parameters
    # uiStateFlow = AccountPreviewMocks -> uiState = AccountPreviewMocks
    new_content = re.sub(r'uiStateFlow\s*=\s*AccountPreviewMocks', 'uiState = AccountPreviewMocks', new_content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime/features'):
    for file in files:
        if 'Screen' in file and file.endswith('.kt'):
            fix_screen_variable_names(os.path.join(root, file))
