# 🎓 Student Biometric Feature Rules
1.  **Identity Matching**: A `StudentEntity` MUST have a matching `StudentBiometricEntity` (1:1 relation via `studentId`).
2.  **Terminology**: Use `StudentBiometric` or `Student` instead of `Face`. CSV exports use `STUDENT ID`.
3.  **AI Models**: The MLKit detection logic lives in `ml/detector/FaceAnalyzer.kt`, but domain logic uses the `Student` terminology.
