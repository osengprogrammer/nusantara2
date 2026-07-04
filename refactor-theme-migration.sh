#!/bin/bash
#
# Theme Migration Refactoring Script
# Moves theme files from app/core/ui/theme to core-designsystem and updates all imports
# Usage: ./refactor-theme-migration.sh [--revert]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="$PROJECT_ROOT/.refactor_backup_$TIMESTAMP"

# Paths
OLD_THEME_DIR="$PROJECT_ROOT/app/src/main/java/com/azuratech/azuratime/core/ui/theme"
NEW_THEME_DIR="$PROJECT_ROOT/core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem/theme"
OLD_PACKAGE="com.azuratech.azuratime.core.ui.theme"
NEW_PACKAGE="com.azuratech.azuratime.core.designsystem.theme"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check for revert mode
if [[ "$1" == "--revert" ]]; then
    if [[ -z "$2" ]]; then
        log_error "Backup directory not specified for revert"
        echo "Usage: $0 --revert <backup_directory>"
        exit 1
    fi
    
    REVERT_DIR="$2"
    if [[ ! -d "$REVERT_DIR" ]]; then
        log_error "Backup directory does not exist: $REVERT_DIR"
        exit 1
    fi
    
    log_warning "Starting revert from: $REVERT_DIR"
    
    # Restore backed up files
    if [[ -d "$REVERT_DIR/app/theme_backup" ]]; then
        log_info "Restoring theme files to original location..."
        cp -r "$REVERT_DIR/app/theme_backup"/* "$OLD_THEME_DIR/" 2>/dev/null || true
    fi
    
    if [[ -d "$REVERT_DIR/designsystem_backup" ]]; then
        log_info "Restoring designsystem files..."
        cp -r "$REVERT_DIR/designsystem_backup"/* "$PROJECT_ROOT/core-designsystem/src/main/java/com/azuratech/" 2>/dev/null || true
    fi
    
    # Restore all modified files
    if [[ -d "$REVERT_DIR/modified_files" ]]; then
        log_info "Restoring ${REVERT_DIR}/modified_files..."
        find "$REVERT_DIR/modified_files" -type f -name "*.kt" | while read -r file; do
            # Get the original path from the restored directory structure
            original_path="${file#$REVERT_DIR/modified_files/}"
            target_path="$PROJECT_ROOT/$original_path"
            target_dir=$(dirname "$target_path")
            
            if [[ ! -d "$target_dir" ]]; then
                mkdir -p "$target_dir"
            fi
            cp "$file" "$target_path"
            log_info "Restored: $original_path"
        done
    fi
    
    log_success "Revert completed successfully!"
    exit 0
fi

# Show help
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Theme Migration Refactoring Script"
    echo ""
    echo "Usage:"
    echo "  $0              Run the migration"
    echo "  $0 --revert <backup_dir>  Revert changes from backup"
    echo "  $0 --help       Show this help"
    echo ""
    echo "This script:"
    echo "  1. Moves theme files from app/.../core/ui/theme to core-designsystem/.../core/designsystem/theme"
    echo "  2. Updates package declarations in moved files"
    echo "  3. Updates all imports in the project"
    echo "  4. Creates a backup for easy revert"
    exit 0
fi

# Step 1: Create backup
log_info "Creating backup at: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR/app/theme_backup"
mkdir -p "$BACKUP_DIR/designsystem_backup"
mkdir -p "$BACKUP_DIR/modified_files"

if [[ -d "$OLD_THEME_DIR" ]]; then
    log_info "Backing up theme directory..."
    cp -r "$OLD_THEME_DIR"/* "$BACKUP_DIR/app/theme_backup/" 2>/dev/null || true
fi

if [[ -d "$PROJECT_ROOT/core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem" ]]; then
    log_info "Backing up existing designsystem directory..."
    cp -r "$PROJECT_ROOT/core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem"/* "$BACKUP_DIR/designsystem_backup/" 2>/dev/null || true
fi

# Find all files that will be modified
log_info "Finding all files with old imports..."
OLD_IMPORT_FILES=$(find "$PROJECT_ROOT" -type f -name "*.kt" -exec grep -l "import $OLD_PACKAGE" {} \; 2>/dev/null)
FILE_COUNT=$(echo "$OLD_IMPORT_FILES" | wc -l)

if [[ "$FILE_COUNT" -eq 0 ]]; then
    log_warning "No files found with old imports: $OLD_PACKAGE"
    log_info "Skipping import updates, but continuing with file moves if needed"
fi

# Back up all modified files
log_info "Backing up ${FILE_COUNT} files that will be modified..."
for file in $OLD_IMPORT_FILES; do
    rel_path="${file#$PROJECT_ROOT/}"
    backup_path="$BACKUP_DIR/modified_files/$rel_path"
    backup_parent=$(dirname "$backup_path")
    
    if [[ ! -d "$backup_parent" ]]; then
        mkdir -p "$backup_parent"
    fi
    
    cp "$file" "$backup_path"
done

log_success "Backup completed. Backup location: $BACKUP_DIR"

# Step 2: Create new theme directory structure
log_info "Creating new theme directory..."
mkdir -p "$NEW_THEME_DIR"

# Step 3: Move theme files
if [[ -d "$OLD_THEME_DIR" ]]; then
    log_info "Moving theme files from $OLD_THEME_DIR to $NEW_THEME_DIR..."
    
    for file in "$OLD_THEME_DIR"/*.kt; do
        if [[ -f "$file" ]]; then
            filename=$(basename "$file")
            log_info "Moving: $filename"
            cp "$file" "$NEW_THEME_DIR/$filename"
        fi
    done
    
    log_success "Theme files moved successfully"
else
    log_warning "Source theme directory does not exist: $OLD_THEME_DIR"
fi

# Step 4: Update package declarations in moved files
log_info "Updating package declarations in new theme files..."

for file in "$NEW_THEME_DIR"/*.kt; do
    if [[ -f "$file" ]]; then
        log_info "Updating package in: $(basename "$file")"
        
        # Read file content
        content=$(cat "$file")
        
        # Replace old package with new package
        new_content=$(echo "$content" | sed "s/package $OLD_PACKAGE/package $NEW_PACKAGE/g")
        
        # Write back to file
        echo "$new_content" > "$file"
    fi
done

log_success "Package declarations updated"

# Step 5: Update imports in ALL files across the project
log_info "Updating imports in all project files..."

TOTAL_UPDATED=0

# Process each file that needs import update
for file in $OLD_IMPORT_FILES; do
    if [[ -f "$file" ]]; then
        rel_path="${file#$PROJECT_ROOT/}"
        log_info "Updating imports in: $rel_path"
        
        # Read the file
        content=$(cat "$file")
        
        # Replace old import with new import
        new_content=$(echo "$content" | sed "s|import $OLD_PACKAGE\.|import $NEW_PACKAGE.|g")
        
        # Only write if content changed
        if [[ "$content" != "$new_content" ]]; then
            echo "$new_content" > "$file"
            ((TOTAL_UPDATED++))
        fi
    fi
done

log_success "Updated imports in $TOTAL_UPDATED files"

# Step 6: Update imports in newly moved theme files (self-references)
log_info "Checking for self-references in new theme files..."
THEME_SELF_REFS=0

for file in "$NEW_THEME_DIR"/*.kt; do
    if [[ -f "$file" ]]; then
        content=$(cat "$file")
        new_content=$(echo "$content" | sed "s|import $OLD_PACKAGE\.|import $NEW_PACKAGE.|g")
        
        if [[ "$content" != "$new_content" ]]; then
            echo "$new_content" > "$file"
            ((THEME_SELF_REFS++))
        fi
    fi
done

if [[ $THEME_SELF_REFS -gt 0 ]]; then
    log_success "Updated $THEME_SELF_REFS self-references in theme files"
fi

# Step 7: Generate summary
log_info "============================================"
log_success "Migration completed successfully!"
log_info "============================================"
echo ""
echo "Summary:"
echo "  - Theme files moved to: $NEW_THEME_DIR"
echo "  - Old package: $OLD_PACKAGE"
echo "  - New package: $NEW_PACKAGE"
echo "  - Files with updated imports: $TOTAL_UPDATED"
echo "  - Backup location: $BACKUP_DIR"
echo ""
echo "Next steps:"
echo "  1. Run: ./gradlew clean :core-designsystem:assembleDebug"
echo "  2. Check for any compilation errors"
echo "  3. Run your app and verify UI components"
echo ""
echo "If something goes wrong, run this command to revert:"
echo "  $0 --revert $BACKUP_DIR"
echo ""
echo "After reverting, switch to the backup directory and delete it manually"
echo ""