#!/bin/bash
#
# Final ONNX Model Deployment Script
# Deploys opset 9 models to Android device
#

set -euo pipefail

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         ONNX Opset 9 Model Deployment Script                  ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Configuration
PACKAGE_NAME="net.ccbluex.liquidbounce"
MODEL_DIR="src/main/resources/resources/liquidbounce/models"

echo "Step 1: Verify models exist"
echo "─────────────────────────────────────────────────────────────────"
if [ ! -f "$MODEL_DIR/19kc8kp.onnx" ] || [ ! -f "$MODEL_DIR/21kc11kp.onnx" ]; then
    echo "ERROR: Models not found in $MODEL_DIR"
    exit 1
fi
echo "✅ Models found:"
ls -lh "$MODEL_DIR"/*.onnx
echo ""

echo "Step 2: Clean previous build"
echo "─────────────────────────────────────────────────────────────────"
./gradlew clean
echo "✅ Build cleaned"
echo ""

echo "Step 3: Build APK with opset 9 models"
echo "─────────────────────────────────────────────────────────────────"
./gradlew build -x test --no-daemon
echo "✅ Build completed"
echo ""

echo "Step 4: Find generated APK"
echo "─────────────────────────────────────────────────────────────────"
APK=$(find build/outputs/apk -name "*.apk" -type f | head -1)
if [ -z "$APK" ]; then
    echo "ERROR: No APK found"
    exit 1
fi
echo "✅ Found APK: $APK"
SIZE=$(du -h "$APK" | cut -f1)
echo "   Size: $SIZE"
echo ""

echo "Step 5: Deploy to device"
echo "─────────────────────────────────────────────────────────────────"
echo "Checking for connected devices..."
adb devices

echo ""
echo "Installing APK..."
adb install -r "$APK"
echo "✅ App installed"
echo ""

echo "Step 6: Verify deployment"
echo "─────────────────────────────────────────────────────────────────"
echo "Launching app..."
adb shell am start -n "$PACKAGE_NAME/.ui.screen.MainScreen" || true
sleep 2

echo ""
echo "Checking logs for model loading..."
echo "(Press Ctrl+C to stop)"
adb logcat | grep -i "onnx\|model\|ir\|opset" | head -20 || true
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    DEPLOYMENT COMPLETE                         ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Next steps:"
echo "1. Check app logs for any model loading errors"
echo "2. Test deep learning features"
echo "3. Verify no 'IR version 13' errors appear"
echo ""
echo "Success criteria: App loads without model errors"
echo ""
