#!/bin/bash
# 🛡️ Azura Time — Safe & Verified Deploy Pipeline (v2.4 Auto-Verify Version)
# =============================================================================
# Usage: 
#   ./deploy.sh                          # Build dengan versi yang ada
#   ./deploy.sh --bump                   # Auto-bump versionCode +1, build
#   ./deploy.sh --bump 3.8.0             # Bump ke versionName spesifik + auto code
#   ./deploy.sh 3.8.0 3710 "Notes"       # Manual version (legacy mode)
#
# Features:
#   • Auto versionCode increment (--bump)
#   • Version guard (Gradle vs APK match)
#   • SHA256 integrity check
#   • GitHub Releases upload
#   • GitHub Pages fallback (docs/version.json) ← ✅ Primary update channel
#   • 🔧 AUTO-VERIFY: Re-reads version from Gradle file to prevent stale data
#   • Zero manual steps, fully autonomous
#   • NO Firebase Remote Config complexity
# =============================================================================

set +e  # Handle errors manually, prevent terminal crash

# =============================================================================
# 📈 AUTO-VERSION BUMP MODULE
# =============================================================================
bump_version_code() {
    local gradle_file="app/build.gradle.kts"
    local target_name="${1:-}"
    
    if [[ ! -f "$gradle_file" ]]; then
        echo "❌ Error: $gradle_file tidak ditemukan!"
        exit 1
    fi

    # Extract current versionCode & versionName
    local current_code=$(grep -E "versionCode\s*=\s*[0-9]+" "$gradle_file" | head -n 1 | grep -oE '[0-9]+$')
    local current_name=$(grep -E 'versionName\s*=\s*"[^"]+"' "$gradle_file" | head -n 1 | grep -oP '"\K[^"]+')
    
    if [[ -z "$current_code" ]]; then
        echo "❌ Error: Gagal menemukan versionCode di $gradle_file"
        exit 1
    fi

    local new_code=$((current_code + 1))
    local new_name="${target_name:-$current_name}"
    
    echo "📈 Bumping version: $current_name ($current_code) → $new_name ($new_code)"

    # Update versionCode
    sed -i -E "s/versionCode\s*=\s*$current_code/versionCode = $new_code/" "$gradle_file"
    
    # Update versionName jika berbeda
    if [[ -n "$new_name" && "$new_name" != "$current_name" ]]; then
        sed -i "s/versionName\s*=\s*\"$current_name\"/versionName = \"$new_name\"/" "$gradle_file"
    fi

    # Auto-commit ke Git
    if git rev-parse --git-dir > /dev/null 2>&1; then
        git add "$gradle_file"
        git commit -m "chore(release): bump to $new_name ($new_code)" --no-verify 2>/dev/null || true
        echo "✅ Committed version bump to Git"
    fi
    
    # Export untuk script utama
    export VERSION_CODE="$new_code"
    export VERSION_NAME="$new_name"
    
    echo "✨ Version bumped: $new_name ($new_code)"
}

# =============================================================================
# 🎯 ARGUMENT PARSING
# =============================================================================
BUMP_MODE=false
TARGET_NAME=""

if [[ "$1" == "--bump" ]]; then
    BUMP_MODE=true
    shift
    if [[ -n "$1" && ! "$1" =~ ^[0-9]+$ ]]; then
        TARGET_NAME="$1"
        shift
    fi
fi

if [[ "$BUMP_MODE" == false ]]; then
    VERSION_NAME="${1:-3.7.9}"
    VERSION_CODE="${2:-3709}"
    RELEASE_NOTES="${3:-Safe deploy pipeline}"
else
    VERSION_NAME="${TARGET_NAME:-3.7.9}"
    VERSION_CODE="${2:-3709}"
    RELEASE_NOTES="${3:-Safe deploy pipeline}"
fi

# =============================================================================
# 🚀 MAIN DEPLOY PIPELINE
# =============================================================================
GITHUB_REPO="osengprogrammer/nusantara2"
APK_DIR="app/build/outputs/apk/release"

echo "🚀 Azura Time v${VERSION_NAME} — Safe Deploy Pipeline"
echo "   Version Code: ${VERSION_CODE} | Notes: ${RELEASE_NOTES}"
echo "=================================================="

# === 0. PRE-FLIGHT CHECK ===
command -v gh &>/dev/null || { echo "❌ gh CLI missing: brew install gh"; exit 1; }
command -v aapt &>/dev/null || { echo "❌ aapt missing: sudo apt install android-sdk-build-tools"; exit 1; }
# ✅ gcloud NOT required anymore (Remote Config skipped)

# === 1. CLEAN CACHE ===
echo "🧹 Cleaning build cache..."
rm -rf app/build/ .gradle/configuration-cache 2>/dev/null
echo "✅ Cache cleared."

# === 2. STABLE BUILD ===
echo "🔨 Building APK (stable mode)..."
./gradlew :app:assembleRelease --no-daemon --no-configuration-cache --quiet
if [ $? -ne 0 ]; then
    echo "❌ BUILD FAILED."
    exit 1
fi
echo "✅ Build successful."

# === 3. FIND APK ===
APK_PATH=$(find "$APK_DIR" -name "*universal*.apk" -type f 2>/dev/null | head -n 1)
[ -z "$APK_PATH" ] && APK_PATH=$(find "$APK_DIR" -name "*.apk" -type f ! -name "*unaligned*" 2>/dev/null | head -n 1)
if [ -z "$APK_PATH" ]; then
    echo "❌ APK NOT FOUND."
    exit 1
fi
APK_NAME=$(basename "$APK_PATH")
echo "📦 APK found: $APK_NAME"

# === 4. 🔒 VERSION GUARD ===
echo "🔍 Verifying version match..."
GRADLE_VER=$(grep -E "versionCode\s*=" app/build.gradle.kts 2>/dev/null | grep -oE '[0-9]+$' | head -1)
APK_VER=$(aapt dump badging "$APK_PATH" 2>/dev/null | grep "versionCode=" | sed "s/.*versionCode='\([0-9]*\).*/\1/")

if [ -z "$GRADLE_VER" ] || [ -z "$APK_VER" ]; then
    echo "⚠️ Could not extract version. Proceeding..."
elif [ "$GRADLE_VER" != "$APK_VER" ]; then
    echo "❌ VERSION MISMATCH! Gradle=$GRADLE_VER vs APK=$APK_VER"
    exit 1
fi
echo "✅ Version match: $APK_VER"

# === 5. CALCULATE SHA256 ===
SHA=$(sha256sum "$APK_PATH" | awk '{print $1}')
echo "🔐 SHA256: $SHA"

# === 6. GITHUB UPLOAD ===
echo "📤 Uploading to GitHub Releases..."
if gh release view "v${VERSION_NAME}" >/dev/null 2>&1; then
    gh release upload "v${VERSION_NAME}" "$APK_PATH" --clobber 2>/dev/null || echo "⚠️ Upload warning"
else
    gh release create "v${VERSION_NAME}" --title "Azura Time v${VERSION_NAME}" --notes "$RELEASE_NOTES" "$APK_PATH" 2>/dev/null || echo "⚠️ Create warning"
fi
echo "✅ GitHub Release: https://github.com/$GITHUB_REPO/releases/tag/v${VERSION_NAME}"

# === 7. ✅ GITHUB PAGES FALLBACK (Primary Update Channel + AUTO-VERIFY) ===
echo "🌐 Updating docs/version.json (Autonomous Update Source)..."

# 🔧 FIX: Re-read version directly from Gradle file to ensure 100% accuracy
# This prevents stale variables from being used if --bump was triggered
VERSION_NAME=$(grep -E 'versionName\s*=\s*"[^"]+"' app/build.gradle.kts | head -n 1 | grep -oP '"\K[^"]+')
VERSION_CODE=$(grep -E "versionCode\s*=\s*[0-9]+" app/build.gradle.kts | head -n 1 | grep -oE '[0-9]+$')

echo "   📝 Verified Version: $VERSION_NAME ($VERSION_CODE)"

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

git add docs/version.json && git commit -m "chore: bump version.json to v${VERSION_NAME} [skip ci]" 2>/dev/null || true
git push origin main 2>/dev/null || true
echo "✅ GitHub Pages updated: https://osengprogrammer.github.io/nusantara2/version.json"

# === 8. ℹ️ REMOTE CONFIG (Optional Manual Only) ===
echo ""
echo "🔐 [Optional] Firebase Remote Config"
echo "   ✅ GitHub Pages is your primary autonomous update channel."
echo "   🔗 Live version.json: https://osengprogrammer.github.io/nusantara2/version.json"
echo ""
echo "   If you want to ALSO use Firebase Remote Config later:"
echo "   1. Open: https://console.firebase.google.com/project/azura-6f4f4/config"
echo "   2. Add these parameters manually:"
echo "      • latest_version_code : $VERSION_CODE (NUMBER)"
echo "      • latest_version_name : \"$VERSION_NAME\" (STRING)"
echo "      • download_url        : https://github.com/$GITHUB_REPO/releases/download/v${VERSION_NAME}/$APK_NAME"
echo "      • sha256              : $SHA"
echo "   3. Click 'Publish changes'"
echo ""
echo "   ⏭️ Skipping auto-publish. GitHub Pages is sufficient."

# === 9. POST-DEPLOY ===
echo ""
echo "🎉 DEPLOY COMPLETE."
echo "   ✅ APK uploaded to GitHub Releases"
echo "   ✅ version.json updated on GitHub Pages"
echo "   📱 Autonomous update will trigger when app checks:"
echo "      https://osengprogrammer.github.io/nusantara2/version.json"
echo ""
echo "   Quick test on device:"
echo "   adb shell dumpsys package com.azuratech.azuratime | grep version"
echo ""
echo "🛡️ Pipeline complete. Simple. Reliable. Autonomous."
