import os
import re

# Pass 1: Collect all Flow/StateFlow/Channel variable names that need renaming
renamed_map = {}

def collect_renames(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Match declarations: val name: Flow, val name = _name.asStateFlow(), etc.
    # We look for word boundaries to extract the name
    pattern = r'\b(?:val|var)\s+(_?[a-zA-Z0-9]+)\s*(?::\s*(?:MutableStateFlow|StateFlow|Flow|MutableSharedFlow|SharedFlow|Channel)|=\s*(?:[a-zA-Z0-9_]+\.(?:asStateFlow|asSharedFlow|receiveAsFlow)\(\)|MutableStateFlow|MutableSharedFlow|Channel))'
    
    for match in re.finditer(pattern, content):
        var_name = match.group(1)
        if not var_name.endswith('Flow') and not var_name.endswith('Channel'):
            if var_name not in ['flow', 'sharedFlow', 'stateFlow', 'channel']:
                renamed_map[var_name] = var_name + 'Flow'

# Scan all files for declarations
for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            collect_renames(os.path.join(root, file))

print(f"Collected {len(renamed_map)} variables to rename.")

# Pass 2: Apply renames globally to ALL files
def apply_renames(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    new_content = content
    # Sort keys by length descending
    for old_name in sorted(renamed_map.keys(), key=len, reverse=True):
        new_name = renamed_map[old_name]
        new_content, count = re.subn(r'\b' + old_name + r'\b', new_name, new_content)
        if count > 0:
            modified = True
    
    if modified:
        # Final cleanup: avoid double FlowFlow
        new_content = new_content.replace('FlowFlow', 'Flow')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

# Apply renames
for root, dirs, files in os.walk('app/src/main/java/com/azuratech/azuratime'):
    for file in files:
        if file.endswith('.kt'):
            if apply_renames(os.path.join(root, file)):
                print(f"Refactored {os.path.join(root, file)}")
