#!/bin/bash
# 🧹 Azura Cleanup — System, Gradle & VS Code Hygiene (v2.1)
# =============================================================================
# Usage: ./cleanup.sh
# Features:
#   • System package cleanup (apt)
#   • Gradle daemon & cache reset (fixes build crashes)
#   • Android build artifact cleanup
#   • VS Code & Copilot cache purge
#   • Log vacuuming
# =============================================================================

set -e

echo "🧹 Azura Cleanup — Starting..."
echo "-----------------------------------------------"

# 1. System Maintenance
echo "🔄 Updating system packages..."
sudo apt update && sudo apt upgrade -y

echo "🗑️ Removing unused dependencies..."
sudo apt autoremove --purge -y
sudo apt autoclean
sudo apt clean

echo "📝 Vacuuming system logs..."
sudo journalctl --vacuum-size=50M

# 2. Gradle & Android Build Cleanup (Crucial for fixing Daemon crashes)
echo "🔨 Cleaning Gradle Daemons & Caches..."
./gradlew --stop 2>/dev/null || true
rm -rf .gradle/configuration-cache
rm -rf .gradle/buildOutputCleanup

echo "📦 Cleaning Android Build Artifacts..."
./gradlew clean --quiet 2>/dev/null || true
rm -rf app/build/outputs/apk/debug/*.apk 2>/dev/null || true
rm -rf app/build/intermediates 2>/dev/null || true

# 3. VS Code & AI Cache Deep Clean
echo "💻 Cleaning VS Code & AI Extension caches..."
# Standard VS Code cache
rm -rf ~/.config/Code/Cache/*
rm -rf ~/.config/Code/CachedData/*
rm -rf ~/.config/Code/User/workspaceStorage/*

# 🔥 Copilot & AI Specific cache
rm -rf ~/.config/Code/User/globalStorage/github.copilot*
rm -rf ~/.config/Code/User/globalStorage/github.copilot-chat*
rm -rf ~/.cache/copilot* 2>/dev/null || true

# 4. Project Specific Junk
echo "🗑️ Removing temporary deploy logs..."
rm -f deploy-log-*.txt
rm -f AzuraTime-v*.apk 2>/dev/null || true # Keep releases in GitHub, not local

echo ""
echo "✅ Azura Cleanup Complete!"
echo "💡 Tip: Run './gradlew assembleDebug' to rebuild fresh."
