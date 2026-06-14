#!/bin/bash
# Helper script to manually update version.json after CI build
# Usage: ./update_version_json.sh v3.7.1

set -e

if [ -z "$1" ]; then
    echo "Usage: ./update_version_json.sh <version_tag>"
    echo "Example: ./update_version_json.sh v3.7.1"
    exit 1
fi

VERSION_TAG="$1"
VERSION_NAME="${VERSION_TAG#v}"  # Remove 'v' prefix

echo "🔍 Fetching release info for $VERSION_TAG..."

# Get download URL and SHA256 from GitHub release
RELEASE_INFO=$(gh release view "$VERSION_TAG" --json assets --jq '.assets[0]')
DOWNLOAD_URL=$(echo "$RELEASE_INFO" | jq -r '.url')
APK_NAME=$(echo "$RELEASE_INFO" | jq -r '.name')

# Download APK to calculate SHA256
TEMP_DIR=$(mktemp -d)
wget -q -O "$TEMP_DIR/$APK_NAME" "$DOWNLOAD_URL"
SHA256=$(sha256sum "$TEMP_DIR/$APK_NAME" | awk '{print $1}')
rm -rf "$TEMP_DIR"

# Extract version code from GitHub
VERSION_CODE=$(gh release view "$VERSION_TAG" --json body --jq '.body' | grep -oP 'versionCode: \K[0-9]+' || echo "0")

# Update version.json
mkdir -p docs
cat > docs/version.json << EOF
{
  "latest_version": "$VERSION_NAME",
  "latest_version_code": $VERSION_CODE,
  "min_supported_version_code": 3600,
  "release_notes": "Release $VERSION_NAME",
  "download_url": "$DOWNLOAD_URL",
  "sha256": "$SHA256"
}
EOF

echo "✅ version.json updated:"
cat docs/version.json

# Commit and push
git add docs/version.json
git commit -m "chore: update version.json to $VERSION_NAME"
git push origin main

echo "🌐 GitHub Pages updated!"
