import os

def fix_imports(file_content):
    lines = file_content.split('\n')
    new_lines = []
    
    for line in lines:
        # General replacements
        line = line.replace('com.azuratech.azuratime.data.core', 'com.azuratech.azuratime.core.data')
        line = line.replace('com.azuratech.azuratime.data.remote', 'com.azuratech.azuratime.features.school.data.remote')
        line = line.replace('com.azuratech.azuratime.domain.model', 'com.azuratech.azuratime.core.domain.model')
        line = line.replace('com.azuratech.azuratime.ui.core', 'com.azuratech.azuratime.core.ui')
        line = line.replace('com.azuratech.azuratime.ui.util', 'com.azuratech.azuratime.core.ui.util')
        line = line.replace('com.azuratech.azuratime.ui.sync', 'com.azuratech.azuratime.core.ui.sync')
        line = line.replace('com.azuratech.azuratime.navigation', 'com.azuratech.azuratime.core.navigation')

        # data.repo replacements
        if 'com.azuratech.azuratime.data.repo' in line:
            if 'AccessRequestRepository' in line:
                line = line.replace('com.azuratech.azuratime.data.repo', 'com.azuratech.azuratime.features.staff.data.repo')
            elif 'WorkspaceRepository' in line:
                line = line.replace('com.azuratech.azuratime.data.repo', 'com.azuratech.azuratime.features.staff.data.repo')
            elif any(x in line for x in ['Boot', 'Main', 'Security', 'Sync', 'SchoolRepository']):
                line = line.replace('com.azuratech.azuratime.data.repo', 'com.azuratech.azuratime.core.data.repo')

        # data.local replacements
        if 'com.azuratech.azuratime.data.local' in line:
            symbols_core = ['AppDatabase', 'Converters', 'DatabaseSeeder', 'UserClassAccessDao', 'UserClassAccessEntity', 'ProfileMappers', 'RawStudentProfile', 'FaceWithDetails', 'FaceCache']
            symbols_school = ['SchoolEntity', 'ClassEntity', 'SchoolClassAssignment', 'SchoolDao', 'SchoolClassDao']
            symbol_staff = 'AccessRequestEntity'
            symbol_attendance = 'AttendanceConflictEntity'
            symbol_biometric = 'FaceAssignmentEntity'

            if any(s in line for s in symbols_core):
                line = line.replace('com.azuratech.azuratime.data.local', 'com.azuratech.azuratime.core.data.local')
            elif any(s in line for s in symbols_school):
                line = line.replace('com.azuratech.azuratime.data.local', 'com.azuratech.azuratime.features.school.data.local')
            elif symbol_staff in line:
                line = line.replace('com.azuratech.azuratime.data.local', 'com.azuratech.azuratime.features.staff.data.local')
            elif symbol_attendance in line:
                line = line.replace('com.azuratech.azuratime.data.local', 'com.azuratech.azuratime.features.attendance.data.local')
            elif symbol_biometric in line:
                line = line.replace('com.azuratech.azuratime.data.local', 'com.azuratech.azuratime.features.biometric.data.local')
            elif line.strip() == 'import com.azuratech.azuratime.data.local.*':
                new_lines.append('import com.azuratech.azuratime.core.data.local.*')
                new_lines.append('import com.azuratech.azuratime.features.school.data.local.*')
                new_lines.append('import com.azuratech.azuratime.features.staff.data.local.*')
                new_lines.append('import com.azuratech.azuratime.features.attendance.data.local.*')
                new_lines.append('import com.azuratech.azuratime.features.biometric.data.local.*')
                continue

        new_lines.append(line)
    
    return '\n'.join(new_lines)

base_path = 'app/src/main/java'
count = 0
for root, dirs, files in os.walk(base_path):
    for file in files:
        if file.endswith('.kt'):
            file_path = os.path.join(root, file)
            with open(file_path, 'r') as f:
                content = f.read()
            
            new_content = fix_imports(content)
            
            if new_content != content:
                with open(file_path, 'w') as f:
                    f.write(new_content)
                count += 1

print(f"Updated {count} files.")
