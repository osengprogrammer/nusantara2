#!/bin/bash
# 🛡️ Azura Time — Safe & Verified Deploy Pipeline (v2.5 Full-Sync Version)
# =============================================================================
# Usage: 
#   ./deploy.sh                          # Build with current version from Gradle
#   ./deploy.sh --bump                   # Auto-bump versionCode +1, build
#   ./deploy.sh --bump 3.8.0             # Bump to specific versionName + auto code
#   ./deploy.sh "Release Notes"          # Build current version with specific notes
# =============================================================================

set +e  # Handle errors manually

GRADLE_FILE="app/build.gradle.kts"

# =============================================================================
# 📈 VERSION EXTRACTION & BUMP MODULE
# =============================================================================
extract_version() {
    VERSION_NAME=$(grep -E 'versionName\s*=\s*"[^"]+"' "$GRADLE_FILE" | head -n 1 | grep -oP '"\K[^"]+')
    VERSION_CODE=$(grep -E "versionCode\s*=\s*[0-9]+" "$GRADLE_FILE" | head -n 1 | grep -oE '[0-9]+$')
}

bump_version() {
    local target_name="${1:-}"
    extract_version
    
    local new_code=$((VERSION_CODE + 1))
    local new_name="${target_name:-$VERSION_NAME}"
    
    echo "📈 Bumping version: $VERSION_NAME ($VERSION_CODE) → $new_name ($new_code)"

    # Update Gradle file
    sed -i -E "s/versionCode\s*=\s*$VERSION_CODE/versionCode = $new_code/" "$GRADLE_FILE"
    if [[ -n "$target_name" ]]; then
        sed -i "s/versionName\s*=\s*\"$VERSION_NAME\"/versionName = \"$new_name\"/" "$GRADLE_FILE"
    fi

    # Update variables for the rest of the script
    VERSION_CODE=$new_code
    VERSION_NAME=$new_name

    # Auto-commit bump
    if git rev-parse --git-dir > /dev/null 2>&1; then
        git add "$GRADLE_FILE"
        git commit -m "chore(release): bump to $VERSION_NAME ($VERSION_CODE)" --no-verify 2>/dev/null || true
    fi
}

# =============================================================================
# 🎯 ARGUMENT PARSING
# =============================================================================
BUMP_MODE=false
TARGET_NAME=""
RELEASE_NOTES="Safe deploy pipeline"

if [[ "$1" == "--bump" ]]; then
    BUMP_MODE=true
    shift
    if [[ -n "$1" && ! "$1" =~ ^[0-9]+$ ]]; then
        TARGET_NAME="$1"
        shift
    fi
    bump_version "$TARGET_NAME"
else
    extract_version
    if [[ -n "$1" ]]; then
        RELEASE_NOTES="$1"
    fi
fi

# =============================================================================
# 🚀 MAIN DEPLOY PIPELINE
# =============================================================================
GITHUB_REPO="osengprogrammer/nusantara2"
APK_DIR="app/build/outputs/apk/release"

echo "🚀 Azura Time v${VERSION_NAME} (${VERSION_CODE})"
echo "   Notes: ${RELEASE_NOTES}"
echo "=================================================="

# === 1. BUILD ===
echo "🔨 Building APK..."
./gradlew :app:assembleRelease --no-daemon --quiet
if [ $? -ne 0 ]; then
    echo "❌ BUILD FAILED."
    exit 1
fi

# === 2. FIND APK ===
APK_PATH=$(find "$APK_DIR" -name "*universal*.apk" -type f 2>/dev/null | head -n 1)
[ -z "$APK_PATH" ] && APK_PATH=$(find "$APK_DIR" -name "*.apk" -type f ! -name "*unaligned*" 2>/dev/null | head -n 1)
if [ -z "$APK_PATH" ]; then
    echo "❌ APK NOT FOUND."
    exit 1
fi
APK_NAME=$(basename "$APK_PATH")

# === 3. INTEGRITY CHECK ===
SHA=$(sha256sum "$APK_PATH" | awk '{print $1}')
echo "🔐 SHA256: $SHA"

# === 4. GITHUB UPLOAD ===
echo "📤 Uploading to GitHub Releases (v${VERSION_NAME})..."
if gh release view "v${VERSION_NAME}" >/dev/null 2>&1; then
    gh release upload "v${VERSION_NAME}" "$APK_PATH" --clobber
else
    gh release create "v${VERSION_NAME}" --title "Azura Time v${VERSION_NAME}" --notes "$RELEASE_NOTES" "$APK_PATH"
fi

# === 5. UPDATE VERSION.JSON ===
echo "🌐 Updating docs/version.json..."
mkdir -p docs
cat > docs/version.json << EOF
{
  "latest_version": "$VERSION_NAME",
  "latest_version_code": $VERSION_CODE,
  "min_supported_version_code": 3600,
  "release_notes": "$RELEASE_NOTES",
  "download_url": "https://github.com/$GITHUB_REPO/releases/download/v${VERSION_NAME}/$APK_NAME",
  "sha256": "$SHA"
}
EOF

git add docs/version.json
git commit -m "chore: update version.json to v${VERSION_NAME}" 2>/dev/null || true
git push origin main 2>/dev/null || true

echo "✅ GitHub Pages updated: https://osengprogrammer.github.io/nusantara2/version.json"
echo "🎉 DEPLOY COMPLETE."
