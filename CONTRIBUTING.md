# 🤖 Azura Time: AI-Native Contribution Guidelines (v3.2.0)

## 🎯 Philosophy
This codebase is optimized for **deterministic AI generation + human review**. Every pattern, naming convention, and architectural decision is designed to eliminate ambiguity, prevent drift, and scale autonomously.

## 🛠 Prerequisites
- JDK 17+
- Android Studio Iguana+ / VS Code + Kotlin Plugin
- Gradle 8.5+
- Run `./gradlew spotlessApply` before every commit

## 🏗 Architecture Principles
1. **Strict MVI**: Every feature uses `UiState` + `UiEvent` + single `onEvent()` entry point.
2. **Local-First SSOT**: Room is the source of truth. Network/Firebase are side-effects.
3. **Result<T> Error Handling**: Repositories return `com.azuratech.azuraengine.result.Result<T>`. No `try/catch` in ViewModels.
4. **Unified Identity**: All user/role references use `Account` + `AccountRole` enum.
5. **Predictable Naming**: `*Flow` suffix for reactive streams, camelCase for navigation routes, PascalCase for classes.

## 📐 Feature Generation Workflow
1. Read `AI_NATIVE_TEMPLATE.md` for exact file structure
2. Generate code using AI with the provided prompt template
3. Run `./gradlew spotlessApply --quiet`
4. Validate with: `./gradlew :app:compileDebugKotlin :app:spotlessCheck`
5. Commit with conventional format: `feat(<module>): <description>`

## 🤖 AI Generation Rules
- ✅ Always use `onEvent(event: UiEvent)` for UI → ViewModel communication
- ✅ Always map repository results via `.onSuccess { }` / `.onFailure { }`
- ✅ Always provide `*PreviewMocks.kt` with `loading()`, `success()`, `error()` factories
- ❌ Never use `LiveData`, `try/catch` in ViewModels, or direct public modifier functions
- ❌ Never hardcode strings in UI. Use `stringResource()` or centralized constants

## 📜 Commit & PR Standards
- Squash commits for features: `feat(attendance): add export filter`
- Run pre-commit hook locally: `./gradlew check --quiet`
- PRs must include: `@Preview` verification, compile success, and architecture compliance

## 🔗 Documentation Index
- `ARCHITECTURE.md` → System design, data flow, layer contracts
- `AI_NATIVE_TEMPLATE.md` → Feature blueprint + AI prompt structure
- `NAMING_CONVENTIONS.md` → Variable, route, and file naming standards
- `GEMINI.md` → AI guardrails & prompt enforcement rules
- `PROJECT_INDEX.md` → Module map + migration status
