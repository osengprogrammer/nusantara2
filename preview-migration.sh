#!/bin/bash
#
# Preview Theme Migration Changes
# Shows what will be changed without making any modifications
# Usage: ./preview-migration.sh
#

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OLD_PACKAGE="com.azuratech.azuratime.core.ui.theme"
NEW_PACKAGE="com.azuratech.azuratime.core.designsystem.theme"

PROJECT_ROOT="$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  Theme Migration Preview${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Step 1: Show files that will be moved
echo -e "${BLUE}1. FILES TO BE MOVED:${NC}"
echo -e "${BLUE}   From: app/src/main/java/com/azuratech/azuratime/core/ui/theme/${NC}"
echo -e "${BLUE}   To:   core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem/theme/${NC}"
echo ""

if [[ -d "$PROJECT_ROOT/app/src/main/java/com/azuratech/azuratime/core/ui/theme" ]]; then
    THEME_FILES=$(find "$PROJECT_ROOT/app/src/main/java/com/azuratech/azuratime/core/ui/theme" -name "*.kt" -type f)
    THEME_COUNT=$(echo "$THEME_FILES" | wc -l)
    
    echo -e "${GREEN}   Found $THEME_COUNT files to move:${NC}"
    for file in $THEME_FILES; do
        filename=$(basename "$file")
        echo "     - $filename"
    done
else
    echo -e "${YELLOW}   No theme directory found. Skipping move step.${NC}"
fi

echo ""

# Step 2: Show files that will be modified
echo -e "${BLUE}2. FILES WITH IMPORTS TO BE UPDATED:${NC}"
echo ""

IMPORT_FILES=$(find "$PROJECT_ROOT" -type f -name "*.kt" -exec grep -l "import $OLD_PACKAGE" {} \; 2>/dev/null)
IMPORT_COUNT=$(echo "$IMPORT_FILES" | wc -l)

if [[ $IMPORT_COUNT -gt 0 ]]; then
    echo -e "${GREEN}   Found $IMPORT_COUNT files to update:${NC}"
    echo ""
    
    # Group by module for better readability
    echo "   BY MODULE:"
    echo "   -----------"
    
    # Group by first path component (module name)
    for module in app feature-* core-*; do
        module_files=$(echo "$IMPORT_FILES" | grep "/$module/" | head -5)
        if [[ -n "$module_files" ]]; then
            count=$(echo "$IMPORT_FILES" | grep "/$module/" | wc -l)
            echo -e "   ${CYAN}$module:${NC} ($count files)"
            echo "$module_files" | while read -r file; do
                rel_path="${file#$PROJECT_ROOT/}"
                echo "     - ${rel_path:0:80}..."
            done
            echo ""
        fi
    done
    
    echo ""
    echo -e "${YELLOW}   All imports will change from:${NC}"
    echo -e "     $RED    import $OLD_PACKAGE.SomeClass${NC}"
    echo -e "   to:${NC}"
    echo -e "     $GREEN    import $NEW_PACKAGE.SomeClass${NC}"
else
    echo -e "${YELLOW}   No files found with old imports.${NC}"
fi

echo ""

# Step 3: Show example changes
echo -e "${BLUE}3. EXAMPLE CHANGES:${NC}"
echo ""

# Find a sample file
SAMPLE_FILE=$(find "$PROJECT_ROOT" -type f -name "*.kt" -exec grep -l "import $OLD_PACKAGE" {} \; 2>/dev/null | head -1)

if [[ -n "$SAMPLE_FILE" ]]; then
    echo "   EXAMPLE FROM: $(basename "$SAMPLE_FILE")"
    echo "   ----------------------------------------"
    echo ""
    
    # Show first 20 lines with imports
    grep -n "import.*$OLD_PACKAGE" "$SAMPLE_FILE" | head -5 | while read -r line; do
        echo "   $RED    $line${NC}"
    done | sed 's/import [^ ]*/import .../g'
    
    echo ""
    echo "   Will become:"
    echo ""
    grep -n "import.*$OLD_PACKAGE" "$SAMPLE_FILE" | head -5 | while read -r line; do
        old_import=$(echo "$line" | grep -o "import [^ ]*" | head -1)
        new_import="${old_import/$OLD_PACKAGE/$NEW_PACKAGE}"
        echo "   $GREEN    Line $(echo $line | cut -d: -f1): $new_import${NC}"
    done
fi

echo ""

# Step 4: Show revert instructions
echo -e "${BLUE}4. REVERT INSTRUCTIONS:${NC}"
echo ""
echo "   After running the migration script, if you encounter issues:"
echo ""
echo "   1. Run: ./refactor-theme-migration.sh --revert <backup_directory>"
echo "   2. The backup directory will be shown at the end of migration"
echo "   3. Example: ./refactor-theme-migration.sh --revert .refactor_backup_20240101_120000"
echo ""

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  Preview Complete${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""
echo "To execute the migration, run:"
echo -e "  ${GREEN}./refactor-theme-migration.sh${NC}"
echo ""
echo "To exit without making changes, just close this terminal"
echo ""