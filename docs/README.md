# 🛡️ Azura Time (v3.2.1-ai-native)

Autonomous attendance & face recognition app optimized for the Indonesian education ecosystem.

## 🏗️ Architecture & Semantic Standard
Azura Time follows a strict **Vertical Slice Architecture (VSA)** and **Effect-Driven MVI** pattern.

### 📐 Terminology Policy
To ensure codebase consistency and maximize AI development speed:
- **Account**: Unified identity model (replaces legacy "User/Staff").
- **Supervisor**: Account restricted to assigned classes (replaces legacy "Teacher").
- **Student**: The person being tracked.
- **Biometric**: Face embeddings and enrollment data.
- **Assignment**: Linking Students to Classes.
- **Membership**: Linking Accounts to Schools.

### ⚡ Technical Standards
- **Language**: 100% Common English for code, comments, and documentation.
- **Reactive**: All `Flow` variables MUST end with the `Flow` suffix.
- **State**: Single `UiState` StateFlow per ViewModel.
- **Effects**: Transient events (Snackbars, Navigation) via `UiEffect` SharedFlow.

## ✨ Key Features
- 📸 **Face-based Attendance**: Powered by Azura Secure Engine (C++).
- 🧬 **Biometric Enrollment**: Seamless student face registration.
- 🔄 **Hybrid Sync**: Multi-class persistence with Firestore ↔️ Room DB reconciliation.
- 🚀 **Supervisor Onboarding**: Dedicated class assignment flow for supervisors.
- 🔐 **Secure Vault**: Hardware-backed integrity verification.
- 🔄 **Autonomous Updates**: GitHub-based in-app update engine.

## 🚀 Quick Start (Dev)

### Prerequisites
- Android SDK 35
- Kotlin 1.9+
- Gradle 8.11
- Firebase Project with Firestore and Storage enabled.

### Build Instructions
```bash
./gradlew assembleDebug
```

---
© 2026 AzuraTech. All rights reserved.
