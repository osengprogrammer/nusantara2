import os
import re

# Pass 1: Collect renamed variables from ViewModels
renamed_map = {}

def collect_renames(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    for line in lines:
        # Match val name: StateFlow... or val name = _name.asStateFlow()
        match = re.search(r'\b(val|var)\s+(_?[a-zA-Z0-9]+)\s*(:?\s*(MutableStateFlow|StateFlow|Flow|MutableSharedFlow|SharedFlow|Channel))', line)
        if match:
            var_name = match.group(2)
            if not var_name.endswith('Flow') and not var_name.endswith('Channel'):
                 if var_name not in ['flow', 'sharedFlow', 'stateFlow']:
                    renamed_map[var_name] = var_name + 'Flow'
        
        match_implicit = re.search(r'\b(val|var)\s+([a-zA-Z0-9]+)\s*=\s*(_?[a-zA-Z0-9]+)\.(asStateFlow|asSharedFlow|receiveAsFlow)\(\)', line)
        if match_implicit:
            var_name = match_implicit.group(2)
            if not var_name.endswith('Flow'):
                renamed_map[var_name] = var_name + 'Flow'

# Scan ViewModels
targets = [
    'app/src/main/java/com/azuratech/azuratime/features',
    'app/src/main/java/com/azuratech/azuratime/core/ui',
    'app/src/main/java/com/azuratech/azuratime/core/boot',
]

for target in targets:
    if os.path.exists(target):
        for root, dirs, files in os.walk(target):
            for file in files:
                if 'ViewModel' in file and file.endswith('.kt'):
                    collect_renames(os.path.join(root, file))

print(f"Collected {len(renamed_map)} renamed variables.")

# Pass 2: Apply renames globally
def apply_renames(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    new_content = content
    # Sort by length descending
    for old_name in sorted(renamed_map.keys(), key=len, reverse=True):
        new_name = renamed_map[old_name]
        # Use word boundaries, but allow viewModel.old_name
        # Be careful not to rename things that are not flow accesses.
        # But since we collected these from ViewModels as Flow types, 
        # it's likely they are meant to be Flows everywhere they are accessed as properties.
        new_content, count = re.subn(r'\b' + old_name + r'\b', new_name, new_content)
        if count > 0:
            modified = True
    
    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

# Apply to all files
for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            if apply_renames(os.path.join(root, file)):
                print(f"Applied renames in {os.path.join(root, file)}")
