import os

files_to_fix = [
    "app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraTextField.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraButton.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/core/designsystem/AzuraCard.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/checkin/CheckInContent.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/report/AttendanceMatrixContent.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt",
    "app/src/main/java/com/azuratech/azuratime/ui/add/FaceListScreen.kt"
]

for filepath in files_to_fix:
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    imports = set()
    other_lines = []
    
    for line in lines:
        if line.startswith('import '):
            imports.add(line)
        else:
            other_lines.append(line)
            
    # Find package line
    package_idx = -1
    for i, line in enumerate(other_lines):
        if line.startswith('package '):
            package_idx = i
            break
            
    if package_idx != -1:
        final_lines = other_lines[:package_idx+1] + ['\n'] + sorted(list(imports)) + other_lines[package_idx+1:]
        with open(filepath, 'w') as f:
            f.writelines(final_lines)

print("Done")
