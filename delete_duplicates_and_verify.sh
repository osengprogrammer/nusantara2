#!/bin/bash
# ============================================================
# Script: delete_duplicates_and_verify.sh
# Purpose: Remove old Account files from :app and verify build
# ============================================================

set -e

echo "🔍 Step 1: Finding and deleting duplicate Account files in :app"

# Find and delete the old files safely (only if they exist)
OLD_ACCOUNT_ENTITY="/home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/data/local/AccountEntity.kt"
OLD_ACCOUNT_DAO="/home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/data/local/AccountDao.kt"
OLD_ACCOUNT_REPO="/home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/domain/repository/AccountRepository.kt"

for file in "$OLD_ACCOUNT_ENTITY" "$OLD_ACCOUNT_DAO" "$OLD_ACCOUNT_REPO"; do
    if [ -f "$file" ]; then
        echo "🗑️  Deleting: $file"
        rm "$file"
    else
        echo "⚠️  Not found (already deleted?): $file"
    fi
done

echo ""
echo "🔍 Step 2: Cleaning up empty directories in :app"

# Remove empty directories (suppress errors if not empty)
rmdir /home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/data/local 2>/dev/null || true
rmdir /home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/data 2>/dev/null || true
rmdir /home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/domain/repository 2>/dev/null || true
rmdir /home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account/domain 2>/dev/null || true
rmdir /home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/account 2>/dev/null || true

echo ""
echo "✅ Duplicate files removed."
echo ""

echo "🔨 Step 3: Compiling core modules to verify architecture"
echo "    Modules: :core-data, :core-auth-api, :core-auth-impl"
echo ""

cd /home/max/azuratime/nusantara-main

./gradlew :core-data:compileDebugKotlin :core-auth-api:compileDebugKotlin :core-auth-impl:compileDebugKotlin --no-daemon 2>&1 | tee /tmp/build_log.txt

echo ""
echo "📊 Build Summary:"
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "✅ SUCCESS: All core modules compiled successfully!"
else
    echo "❌ FAILURE: Check /tmp/build_log.txt for errors."
    echo ""
    echo "Common issues to look for:"
    echo "  - 'Unresolved reference: AccountEntity' → Check imports in :core-data"
    echo "  - 'Duplicate class' → Old files still exist in :app"
    echo "  - 'dagger.hilt.android' → Missing Hilt dependency in :core-data"
fi

echo ""
echo "🚀 Next steps if successful:"
echo "   1. Run: ./gradlew :app:compileDebugKotlin to verify :app still works"
echo "   2. Update :app/build.gradle.kts to depend on :core-data"
echo "   3. Remove old references to local Account files in :app"