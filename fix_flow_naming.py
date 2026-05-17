import os
import re

def rename_flow_vars(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    modified = False
    new_lines = []
    
    # Map of old names to new names in this file
    name_map = {}
    
    # First pass: find all Flow/StateFlow declarations that need renaming
    for line in lines:
        # Match private val _name: Flow... or val name: Flow...
        # Also match val name = _name.asStateFlow()
        match = re.search(r'\b(val|var)\s+(_?[a-zA-Z0-9]+)\s*(:?\s*(MutableStateFlow|StateFlow|Flow|MutableSharedFlow|SharedFlow|Channel))', line)
        if match:
            var_name = match.group(2)
            if not var_name.endswith('Flow') and not var_name.endswith('Channel'):
                # Avoid renaming things like 'flow' or 'MutableStateFlow' itself
                if var_name not in ['flow', 'sharedFlow', 'stateFlow']:
                    name_map[var_name] = var_name + 'Flow'
        
        # Also match implicit types: val name = _name.asStateFlow()
        match_implicit = re.search(r'\b(val|var)\s+([a-zA-Z0-9]+)\s*=\s*(_?[a-zA-Z0-9]+)\.(asStateFlow|asSharedFlow|receiveAsFlow)\(\)', line)
        if match_implicit:
            var_name = match_implicit.group(2)
            if not var_name.endswith('Flow'):
                name_map[var_name] = var_name + 'Flow'

    if not name_map:
        return False

    # Second pass: apply replacements
    for line in lines:
        new_line = line
        # Sort keys by length descending to avoid partial replacements (e.g., _uiState before uiState)
        for old_name in sorted(name_map.keys(), key=len, reverse=True):
            new_name = name_map[old_name]
            # Use word boundaries
            new_line = re.sub(r'\b' + old_name + r'\b', new_name, new_line)
        
        if new_line != line:
            modified = True
        new_lines.append(new_line)
    
    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        return True
    return False

# Target directories
targets = [
    'app/src/main/java/com/azuratech/azuratime/features',
    'app/src/main/java/com/azuratech/azuratime/core/ui',
    'app/src/main/java/com/azuratech/azuratime/core/boot',
    'app/src/main/java/com/azuratech/azuratime/core/data/repo',
]

for target in targets:
    if os.path.exists(target):
        for root, dirs, files in os.walk(target):
            for file in files:
                if file.endswith('.kt'):
                    path = os.path.join(root, file)
                    if rename_flow_vars(path):
                        print(f"Renamed Flows in {path}")
