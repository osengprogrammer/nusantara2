import os

replacements = {
    "CheckInLocalDataSource": "AttendanceLocalDataSource",
    "CheckInRemoteDataSource": "AttendanceRemoteDataSource",
    "CheckInRepository": "AttendanceRepository",
    "CheckInResult": "AttendanceResult",
    "CheckInUiState": "AttendanceUiState",
    "CheckInRecord": "AttendanceRecord",
    "CheckInStatus": "AttendanceStatus",
    "CheckInSideEffect": "AttendanceSideEffect",
    "ScannerViewModel": "AttendanceScannerViewModel",
    "checkInRecordDao": "attendanceRecordDao",
}

for root, dirs, files in os.walk("app/src/main/java/com/azuratech/azuratime/features/attendance"):
    for file in files:
        if file.endswith(".kt"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_content = content
            for old, new in replacements.items():
                new_content = new_content.replace(old, new)
            
            if content != new_content:
                print(f"Internal renaming in {path}")
                with open(path, "w", encoding="utf-8") as f:
                    f.write(new_content)
