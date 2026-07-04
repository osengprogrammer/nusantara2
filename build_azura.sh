#!/bin/bash

# Build Script for AzuraTime - Versi Cerdas
# Penggunaan: ./build_azura.sh [debug|release]

MODE=${1:-debug} # Default ke debug jika tidak ada argumen

echo "Starting AzuraTime Build Process (Mode: $MODE)..."

# 1. Clean
./gradlew clean

# 2. Build Flavor (School & Office)
if [ "$MODE" == "release" ]; then
    echo "Building RELEASE variants..."
    ./gradlew assembleSchoolRelease assembleOfficeRelease -x test -x lint
else
    echo "Building DEBUG variants..."
    ./gradlew assembleSchoolDebug assembleOfficeDebug -x test -x lint
fi

# 3. Result info
echo "Build Finished!"
if [ "$MODE" == "release" ]; then
    echo "School APK: app/build/outputs/apk/school/release/app-school-release.apk"
    echo "Office APK: app/build/outputs/apk/office/release/app-office-release.apk"
else
    echo "School APK: app/build/outputs/apk/school/debug/app-school-debug.apk"
    echo "Office APK: app/build/outputs/apk/office/debug/app-office-debug.apk"
fi
