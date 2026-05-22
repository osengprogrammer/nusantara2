#!/bin/bash
# 🚀 Azura Time - Feature Test Script (v3.2.0-ai-native)
# This script automates the deployment and log monitoring for the AppUpdate feature.

echo "🧹 Clearing logcat buffer..."
adb logcat -c

echo "📦 Uninstalling old version..."
adb uninstall com.azuratech.azuratime 2>/dev/null

echo "📥 Installing fresh debug build..."
adb install app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "🚀 Launching MainActivity..."
    adb shell am start -n com.azuratech.azuratime/.MainActivity
    
    echo "🔍 Monitoring AppUpdate logs (Ctrl+C to stop)..."
    adb logcat | grep -iE "AppUpdate|UpdateCheck|onEvent|version\.json|FATAL|AndroidRuntime|com\.azuratech\.azuratime.*: E"
else
    echo "❌ Installation failed. Please ensure a device is connected via ADB."
fi
