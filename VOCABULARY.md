# 📖 AzuraTime Semantic Vocabulary (v3.2.2-ai-native)
⚡ *Mandatory semantic reference for all AI agents. Prevents domain ambiguity and terminology regression.*

## 🔹 Core Taxonomy

| Term | Semantic Definition | Technical Context |
| :--- | :--- | :--- |
| **Account** | The unified identity model for application users. | `AccountEntity`, `AccountRepository` |
| **Admin** | An account with full management rights for a specific School. | `AccountRole.ADMIN`, `PermissionUtils.isAdmin()` |
| **Supervisor** | An account restricted to managing specific assigned classes. | `AccountRole.SUPERVISOR`, `PermissionUtils.isSupervisorOf()` |
| **Student** | The person being recorded or tracked in the system. | `StudentEntity`, `StudentRepository` |
| **School** | A multi-tenant workspace/institution context. | `SchoolEntity`, `SchoolRepository` |
| **Class** | A logical grouping of students within a school (e.g., "10-IPA-1"). | `ClassEntity`, `ClassDao` |
| **Biometric** | Face embeddings and native matching data. | `StudentBiometricEntity`, `BiometricRepository` |
| **Assignment** | The many-to-many link between a Student and a Class. | `StudentClassAssignmentEntity` |
| **Membership** | The link between an Account and a School workspace. | `SchoolMembership` (nested in `AccountEntity`) |

---

## 🔹 Semantic Nuances (Distinctions)

### 1. Membership vs. Assignment
- **Membership:** Links an **Account** to a **School**. It defines "Who can log in to this school and what is their role (Admin/Supervisor)?"
- **Assignment:** Links a **Student** to a **Class**. It defines "Which class session does this student belong to for attendance?"

### 2. Supervisor vs. Admin
- **Admin:** School-bound. Has authority over the entire school workspace, including class creation and account verification.
- **Supervisor:** Class-bound. Only has authority over students in their `assignedClassIds`. They are the "front-line" users taking attendance.

### 3. Sync vs. Push vs. Pull
- **Pull:** Unidirectional data flow from **Cloud (Firestore)** to **Local (Room)**.
- **Push:** Unidirectional data flow from **Local (Room)** to **Cloud (Firestore)**.
- **Sync:** Bidirectional reconciliation logic that ensures both sources match (typically involves a Pull followed by a conditional Push).

---

## 🚫 Forbidden Synonyms & Semantic Purge (Terminology Graveyard)
*NEVER use these terms in code, comments, or prompts. If found, refactor immediately.*

- **Identity & Role Purge Rules**:
  - **`USER` / `User`**: **STRICTLY FORBIDDEN** (except for Firebase internal `Auth` user handles or standard library system wrappers). Use **`Account`** for user context and **`GUEST`** for unauthenticated/fallback roles.
  - **`TEACHER` / `Teacher`**: **STRICTLY FORBIDDEN** in all logical gates and models. Use **`Supervisor`** instead.
  - **Only Authorized Roles**: **`GUEST`**, **`SUPERVISOR`**, **`ADMIN`**, and **`SUPER_ADMIN`** are the only authorized identity/state roles in local database schemas, permission managers, and ViewModels.

| Forbidden Term | Use This Instead | Reasoning |
| :--- | :--- | :--- |
| `User` | **Account** (context) / **GUEST** (role) | "User" is too generic, often confused with Firebase Auth User, and violates our semantic purity standard. |
| `Staff` | **Account** | Legacy term from v2.0. |
| `Teacher` | **Supervisor** | "Teacher" implies a job title; "Supervisor" describes the system role in our VSA. |
| `Classroom` | **Class** | Standardized to "Class" to match KMP engine models. |
| `Institution` | **School** | Too verbose. "School" is the canonical domain term. |
| `GradeLevel` | **Grade** | Redundant. |
| `Personnel` | **Student** | Standardized to "Student" across all sectors for schema parity. |
| `Record` | **AttendanceRecord** | "Record" is too generic (could mean DB record). |

---

## 🤖 AI Implementation Rule
When an AI agent sees a task like "Add teacher management", it MUST translate it to "Refine Account Management for Supervisors" using the logic:
1.  Map `Teacher` → `Supervisor`.
2.  Map `Management` → `AccountManagement`.
3.  Ensure `assignedClassIds` are handled in `SchoolMembership`.
