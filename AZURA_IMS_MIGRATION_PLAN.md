# 🛡️ AZURA IMS - ARCHITECTURAL MIGRATION PLAN (v1.0)
⚡ *Standard: 100% AI-Native | 100% MVI | 100% DRY | Effect-Driven*

## 🎯 Project Goal
Transform "Azura Time" (Attendance System) into **"Azura IMS" (Inventory Management System)** by reusing the robust Phase 13 architecture.

---

## 🗺️ Domain Mapping (The "Logic Bridge")
Future AI Agent: Use this table for all renaming and logic transformation tasks.

| Legacy Entity (Azura Time) | New Entity (Azura IMS) | Property Changes |
| :--- | :--- | :--- |
| `Student` | `Material` (Bahan) | Add `unit`, `minStock`, `currentStock`, `sku`. |
| `Class` | `Category` | Add `description`. |
| `AttendanceRecord` | `StockTransaction` | Change `status` to `type` (IN/OUT/ADJ). Add `quantity`. |
| `StudentRegistration` | `StockInput` | Focus on bulk quantity updates via CSV. |
| `Attendance Log` | `Stock Ledger` | Detailed audit of all stock movements. |
| `face_id` | `material_id` | Unique identifier for inventory items. |

---

## 🛠️ Phase 1: Infrastructure & Package Migration
1. **Package Rename**: Global refactor from `com.azuratech.azuratime` to `com.azuratech.azuraims`.
2. **Namespace Update**: Update `build.gradle.kts` (namespace & applicationId).
3. **SSOT Standard**: Preserve the `asLocalResult()` engine in `Result.kt`.
4. **Zohar AI Tuning**: Update Zohar's context to handle inventory queries (Stock-outs, low stock alerts).

## 🗄️ Phase 2: Schema Evolution (Room)
1. **MaterialEntity**: 
    - `id`, `name`, `categoryId`, `uom` (Unit of Measure), `currentStock`, `lastPrice`.
2. **StockTransactionEntity**: 
    - `id`, `materialId`, `quantity`, `type` (IN/OUT), `timestamp`, `accountId`.
3. **CategoryEntity**: 
    - `id`, `name`, `schoolId` (Mapped to Warehouse/Store ID).

## 🧠 Phase 3: Repository & MVI Standardization
1. **MaterialRepository**: 
    - Implement `observeMaterials()`, `saveMaterial()`, `updateStock(diff)`.
    - Must use `asLocalResult()` extension.
2. **InventoryRepository**: 
    - Implement `recordTransaction()`, `observeLedger()`.
3. **MVI Contracts**: 
    - Every new feature MUST have `UiState`, `UiEffect`, and `UiEvent`.
    - transient events (Toast/Nav) MUST go through `UiEffect`.

## 🖥️ Phase 4: UI Standardisation
1. **Azura Design System**: Maintain `AzuraScreen`, `AzuraShapes`, and `AzuraSpacing`.
2. **Icons**: Use `Inventory`, `Category`, `History` icons (AutoMirrored).
3. **Bulk Import**: Reuse `CsvImportUtils` with new headers: `material_id, name, category_name, initial_quantity`.

---

## 🤖 Special Instructions for AI Agent
- **Standard**: Follow `v3.2.0-ai-native` markers in every file header.
- **MVI**: Strictly implement `SharedFlow<UiEffect>` in ViewModels.
- **DRY**: Do not write manual Result mapping; use `.asLocalResult()`.
- **Validation**: Every refactor must be followed by `./gradlew :app:compileDebugKotlin`.

**JOSS GANDOS! Let's build the ultimate IMS.**
