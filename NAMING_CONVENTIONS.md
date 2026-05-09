# 📐 NUSANTARA v3.1 — AI-OPTIMIZED NAMING CONVENTIONS
⚡ *Mandatory for all AI prompts & code generation.*

## 🔹 FILE & CLASS NAMING
- Files: `PascalCase.kt` → `FaceRepository.kt`, `CheckInViewModel.kt`
- Classes/Objects: `PascalCase` → `class StudentProfile`, `object UiEvent`
- Packages: `lowercase.dot.separated` → `data.repo`, `ui.checkin`

## 🔹 FUNCTION & METHOD NAMING
- Functions: `camelCase` + **verb-first** → `syncFaces()`, `getUserById()`, `observeActiveSchool()`
- DAOs: `observeXxx()`, `insertXxx()`, `upsertXxx()`, `deleteXxx()`
- Repos: `syncXxx()`, `saveXxx()`, `getXxx()`, `observeXxx()`
- ViewModels: `refreshXxx()`, `handleXxxAction()`, `navigateToXxx()`

## 🔹 VARIABLE & PROPERTY NAMING
- Variables: `camelCase` + **noun-first** → `activeSchoolId`, `currentUser`, `syncStatus`
- Constants: `UPPER_SNAKE_CASE` → `MAX_RETRY_COUNT`, `DB_VERSION`
- Flows: Suffix with `Flow` → `schoolIdFlow`, `faceListFlow`
- StateFlows: Suffix with `State` → `uiState`, `syncState`, `loadingState`

## 🔹 STRICT AI RULES
1. NEVER use `snake_case` in Kotlin identifiers
2. NEVER start functions with uppercase (`SaveUser` ❌ → `saveUser` ✅)
3. ALWAYS suffix reactive streams with `Flow` or `State`
4. ALWAYS use verb-first for functions, noun-first for variables
5. If unsure → check this file first. When in doubt, match existing layer pattern.

## 🤖 AI PROMPT TEMPLATE
"Follow NAMING_CONVENTIONS.md. Use camelCase functions (verb-first), PascalCase files/classes, suffix Flows with 'Flow'. Zero underscores in Kotlin code. If renaming, use IDE safe-refactor only."
