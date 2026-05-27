#!/bin/bash
set -e
echo "🚀 Azura Time v3.7.1 — Automated Deploy Pipeline"

# === CONFIG ===
VERSION_NAME="3.7.2"
VERSION_CODE=3702
GITHUB_REPO="osengprogrammer/nusantara2"
GITHUB_PAGES_URL="https://osengprogrammer.github.io/nusantara2"

# === 1. BUILD ===
echo "🔨 Building release APK..."
./gradlew clean assembleRelease --quiet
APK_PATH="app/build/outputs/apk/release/app-release.apk"
APK_NAME="AzuraTime-v${VERSION_NAME}.apk"

# === 2. GITHUB RELEASE ===
echo "📤 Uploading to GitHub Release..."
gh release upload "v${VERSION_NAME}" "$APK_PATH" --clobber 2>/dev/null || \
gh release create "v${VERSION_NAME}" --title "Azura Time v${VERSION_NAME}" --notes "Auto-deploy" "$APK_PATH"

# === 3. UPDATE version.json ===
echo "🌐 Updating version.json..."
SHA=$(sha256sum "$APK_PATH" | awk '{print $1}')
cat > docs/version.json << EOF
{
  "latest_version": "$VERSION_NAME",
  "latest_version_code": $VERSION_CODE,
  "min_supported_version_code": 3600,
  "release_notes": "MVI ✅, FCM ✅, Theme ✅",
  "download_url": "https://github.com/$GITHUB_REPO/releases/latest/download/$APK_NAME",
  "sha256": "$SHA"
}
EOF
git add docs/version.json && git commit -m "chore: bump version.json [skip ci]" || true
git push origin main

# === 4. FINAL STATUS ===
echo ""
echo "🎉 DEPLOY COMPLETE"
echo "🌐 $GITHUB_PAGES_URL"
echo "📦 APK: https://github.com/$GITHUB_REPO/releases/latest/download/$APK_NAME"Not Found
