import os
import re

base_dir = "app/src/main/java"
start_package = "com.azuratech.azuratime"

mismatches = []

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            file_path = os.path.join(root, file)
            
            # Calculate expected package
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

for path, actual, expected in mismatches:
    print(f"{path}|{actual}|{expected}")
