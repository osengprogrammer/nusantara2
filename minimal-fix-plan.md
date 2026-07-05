# Minimal Fix Plan

## Problem
- `AuthRepositoryImpl.kt` was replaced with a stub
- Old compiled class with 8-parameter constructor still in build cache
- `:core-data` module is empty (missing infrastructure files)
- KSP fails with `error.NonExistentClass` for missing types

## Solution Approach
Instead of reconstructing the full infrastructure, create minimal stub files to satisfy the build:

## Required Files
1. `/core-data/src/main/java/com/azuratech/azuratime/core/data/local/AppDatabase.kt`
2. `/core-data/src/main/java/com/azuratech/azuratime/core/data/local/account/AccountEntity.kt`
3. `/core-data/src/main/java/com/azuratech/azuratime/core/domain/repository/account/AccountRepository.kt`
4. `/core-data/src/main/java/com/azuratech/azuratime/core/domain/model/SyncStatus.kt`

## Approach
- Create these files with minimal implementations
- Focus on satisfying type signatures, not full functionality
- Allow build to proceed to clean stale cache

## Next Steps
1. Clean build: `./gradlew clean`
2. Compile: `./gradlew :core-auth-impl:compileDebugKotlin`
3. Verify success or identify next missing piece

