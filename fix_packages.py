import os
import re

base_dir = "app/src/main/java"

def get_mismatches():
    mismatches = []
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith(".kt"):
                file_path = os.path.join(root, file)
                rel_path = os.path.relpath(root, base_dir)
                expected_package = rel_path.replace(os.sep, ".")
                
                with open(file_path, 'r') as f:
                    content = f.read()
                    package_match = re.search(r'^package\s+([\w\.]+)', content, re.MULTILINE)
                    if package_match:
                        actual_package = package_match.group(1)
                        if actual_package != expected_package:
                            mismatches.append((file_path, actual_package, expected_package))
                    else:
                        mismatches.append((file_path, None, expected_package))
    return mismatches

mismatches = get_mismatches()

# Mapping of (OldFullClassName -> NewFullClassName)
# Also mapping (OldPackage -> NewPackage) for wildcard imports
class_updates = {}
package_updates = {}

for file_path, actual_pkg, expected_pkg in mismatches:
    file_name = os.path.basename(file_path).replace(".kt", "")
    
    if actual_pkg:
        old_full_name = f"{actual_pkg}.{file_name}"
        new_full_name = f"{expected_pkg}.{file_name}"
        class_updates[old_full_name] = new_full_name
        
        # Track package moves for wildcard imports or general package-level references
        if actual_pkg not in package_updates:
            package_updates[actual_pkg] = set()
        package_updates[actual_pkg].add(expected_pkg)

# 1. Update package declarations
for file_path, actual_pkg, expected_pkg in mismatches:
    with open(file_path, 'r') as f:
        content = f.read()
    
    if actual_pkg:
        new_content = content.replace(f"package {actual_pkg}", f"package {expected_pkg}", 1)
    else:
        # If no package declaration, add it at the top
        new_content = f"package {expected_pkg}\n\n" + content
    
    with open(file_path, 'w') as f:
        f.write(new_content)
    print(f"Updated package in {file_path}")

# 2. Update imports in all .kt files
all_kt_files = []
for root, dirs, files in os.walk("app/src"):
    for file in files:
        if file.endswith(".kt"):
            all_kt_files.append(os.path.join(root, file))

for kt_file in all_kt_files:
    with open(kt_file, 'r') as f:
        content = f.read()
    
    original_content = content
    
    # Update specific class imports
    for old_full, new_full in class_updates.items():
        # Replace 'import old_full' with 'import new_full'
        content = content.replace(f"import {old_full}", f"import {new_full}")
        # Also replace fully qualified names in code (though less common)
        # content = re.sub(r'(?<![a-zA-Z0-9_.])' + re.escape(old_full) + r'(?![a-zA-Z0-9_.])', new_full, content)

    if content != original_content:
        with open(kt_file, 'w') as f:
            f.write(content)
        print(f"Updated imports in {kt_file}")
