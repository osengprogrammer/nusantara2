#!/bin/bash
#
# Post-Migration Validation Script
# Checks if all imports have been updated correctly
# Usage: ./validate-migration.sh
#

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OLD_PACKAGE="com.azuratech.azuratime.core.ui.theme"
NEW_PACKAGE="com.azuratech.azuratime.core.designsystem.theme"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Post-Migration Validation${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

ERRORS=0
WARNINGS=0

# Check 1: Verify no old imports remain
echo -e "${BLUE}1. CHECKING FOR REMAINING OLD IMPORTS...${NC}"
OLD_IMPORTS=$(find "$PROJECT_ROOT" -type f -name "*.kt" -exec grep -l "import $OLD_PACKAGE" {} \; 2>/dev/null)

if [[ -n "$OLD_IMPORTS" ]]; then
    echo -e "${RED}   ERROR: Found files with old imports:${NC}"
    echo "$OLD_IMPORTS" | while read -r file; do
        rel_path="${file#$PROJECT_ROOT/}"
        echo "     - $rel_path"
        grep -n "import $OLD_PACKAGE" "$file" | head -3
    done
    ERRORS=$((ERRORS + $(echo "$OLD_IMPORTS" | wc -l)))
else
    echo -e "${GREEN}   ✓ No old imports found${NC}"
fi

echo ""

# Check 2: Verify new theme files exist
echo -e "${BLUE}2. CHECKING NEW THEME FILES...${NC}"
NEW_THEME_DIR="$PROJECT_ROOT/core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem/theme"

if [[ -d "$NEW_THEME_DIR" ]]; then
    THEME_FILES=$(find "$NEW_THEME_DIR" -name "*.kt" -type f)
    THEME_COUNT=$(echo "$THEME_FILES" | wc -l)
    
    if [[ $THEME_COUNT -gt 0 ]]; then
        echo -e "${GREEN}   ✓ Found $THEME_COUNT theme files in new location${NC}"
        
        # Check package declarations
        WRONG_PACKAGE=$(grep -L "package $NEW_PACKAGE" "$THEME_FILES" 2>/dev/null | wc -l)
        if [[ $WRONG_PACKAGE -gt 0 ]]; then
            echo -e "${YELLOW}   WARNING: $WRONG_PACKAGE files have incorrect package declaration${NC}"
            WARNINGS=$((WARNINGS + WRONG_PACKAGE))
        else
            echo -e "${GREEN}   ✓ All theme files have correct package declaration${NC}"
        fi
    else
        echo -e "${RED}   ERROR: No theme files found in new location${NC}"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo -e "${RED}   ERROR: New theme directory does not exist${NC}"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# Check 3: Verify old theme directory is empty or removed
echo -e "${BLUE}3. CHECKING OLD THEME DIRECTORY...${NC}"
OLD_THEME_DIR="$PROJECT_ROOT/app/src/main/java/com/azuratech/azuratime/core/ui/theme"

if [[ -d "$OLD_THEME_DIR" ]]; then
    OLD_FILES=$(find "$OLD_THEME_DIR" -name "*.kt" -type f 2>/dev/null | wc -l)
    if [[ $OLD_FILES -gt 0 ]]; then
        echo -e "${YELLOW}   WARNING: Old theme directory still has $OLD_FILES files${NC}"
        echo "   Consider removing: $OLD_THEME_DIR"
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "${GREEN}   ✓ Old theme directory is empty (can be safely removed)${NC}"
    fi
else
    echo -e "${GREEN}   ✓ Old theme directory removed${NC}"
fi

echo ""

# Check 4: Check for broken imports (files that import from new location but file doesn't exist)
echo -e "${BLUE}4. CHECKING FOR BROKEN IMPORTS...${NC}"
BROKEN_IMPORTS=0

# Get all files from new package
NEW_PACKAGE_FILES=$(find "$NEW_THEME_DIR" -name "*.kt" -type f 2>/dev/null | xargs -I {} basename {} .kt 2>/dev/null)

# Check if any file tries to import a class from the new package that doesn't exist
for class_name in $NEW_PACKAGE_FILES; do
    valid_class=$(find "$NEW_THEME_DIR" -name "${class_name}.kt" -type f 2>/dev/null)
    if [[ -z "$valid_class" ]]; then
        echo -e "${YELLOW}   WARNING: No file found for class: $class_name${NC}"
        BROKEN_IMPORTS=$((BROKEN_IMPORTS + 1))
    fi
done

if [[ $BROKEN_IMPORTS -eq 0 ]]; then
    echo -e "${GREEN}   ✓ All imported classes exist${NC}"
else
    echo -e "${YELLOW}   WARNING: $BROKEN_IMPORTS potentially broken class references${NC}"
    WARNINGS=$((WARNINGS + BROKEN_IMPORTS))
fi

echo ""

# Check 5: Try to build
echo -e "${BLUE}5. ATTEMPTING BUILD...${NC}"
echo "   Running: ./gradlew :core-designsystem:assembleDebug --quiet"

cd "$PROJECT_ROOT"
if ./gradlew :core-designsystem:assembleDebug --quiet > /dev/null 2>&1; then
    echo -e "${GREEN}   ✓ Build successful!${NC}"
else
    echo -e "${RED}   ERROR: Build failed!${NC}"
    echo "   Run: ./gradlew :core-designsystem:assembleDebug --info"
    echo "   for detailed error information"
    ERRORS=$((ERRORS + 1))
fi

echo ""

# Summary
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Validation Summary${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

if [[ $ERRORS -eq 0 ]]; then
    if [[ $WARNINGS -eq 0 ]];
    then
        echo -e "${GREEN}   ✓✓✓ ALL CHECKS PASSED! ✓✓✓${NC}"
        echo ""
        echo "   Your migration is complete and successful!"
        echo "   You can now safely remove the backup directories."
    else
        echo -e "${GREEN}   ✓ Migration successful with $WARNINGS warning(s)${NC}"
        echo ""
        echo "   Review the warnings above, but your code should work."
    fi
else
    echo -e "${RED}   ✗✗✗ $ERRORS ERROR(S) FOUND ✗✗✗${NC}"
    echo ""
    echo "   IMPORTANT: Fix the errors before proceeding!"
    echo "   You can revert using:"
    echo "   ./refactor-theme-migration.sh --revert <backup_directory>"
fi

echo ""

if [[ $WARNINGS -gt 0 ]]; then
    echo -e "${YELLOW}   WARNINGS: ${WARNINGS}${NC}"
fi

echo ""