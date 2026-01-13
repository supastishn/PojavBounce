# ONNX Model Deployment Checklist

## 🚀 Ready to Deploy Opset 9 Models

Your opset 9 compatible ONNX models are generated and ready to deploy!

### Generated Models Location
```
./generated-opset9-models/
  ├─ 19kc8kp.onnx (52 KB) ✅
  └─ 21kc11kp.onnx (52 KB) ✅
```

---

## Pre-Deployment Checklist

### Security (⚠️ MUST DO FIRST)
- [ ] Revoke exposed GitHub token: https://github.com/settings/tokens
- [ ] Generate new token with scopes: `repo`, `workflow`, `actions:read`
- [ ] Update `~/.github_token` with new token
- [ ] Verify token is NOT in git: `cat .gitignore | grep token`

### Preparation
- [ ] Identify your app's model directory location
  - Check: `src/main/resources/resources/liquidbounce/models/`
  - Or search codebase for "model" path references
- [ ] Backup existing models (if any)
  - `mkdir -p models_backup && cp <app-models>/*.onnx models_backup/`
- [ ] Have Android SDK/ADB ready for deployment
- [ ] Ensure device is accessible via ADB (or prepared for manual install)

---

## Deployment Steps

### Step 1: Copy Models to App ✓
```bash
# Copy the opset 9 models to your app's models folder
cp ./generated-opset9-models/*.onnx <YOUR_APP_MODELS_PATH>/

# Verify they're there
ls -lah <YOUR_APP_MODELS_PATH>/*.onnx
```

Checklist:
- [ ] Both .onnx files copied
- [ ] File sizes ~50KB each
- [ ] Files are readable

### Step 2: Clean Build ✓
```bash
# Clean previous build artifacts
./gradlew clean

# Build the app
./gradlew build
```

Checklist:
- [ ] Build completes without errors
- [ ] No ONNX-related compilation warnings
- [ ] APK generated in `build/outputs/apk/`

### Step 3: Deploy to Device ✓

**Option A: Via ADB (Recommended)**
```bash
# Install on connected device
adb install -r build/outputs/apk/release/app-release.apk

# Or if using debug APK:
adb install -r build/outputs/apk/debug/app-debug.apk
```

Checklist:
- [ ] Device connected via USB
- [ ] ADB recognizes device: `adb devices`
- [ ] Installation completes successfully
- [ ] No "signature mismatch" errors

**Option B: Manual Installation**
```bash
# Transfer APK to device
adb push build/outputs/apk/release/app-release.apk /sdcard/

# Then: File Manager → Install APK
```

Checklist:
- [ ] APK transferred to device
- [ ] File manager opens APK
- [ ] Install succeeds
- [ ] App permissions granted

**Option C: Android Studio**
```
Run → Edit Configurations → Select app → Click Run
```

Checklist:
- [ ] App targets correct API level
- [ ] Target device selected
- [ ] Installation succeeds

### Step 4: Verify the Fix ✓

```bash
# Check logs for model loading
adb logcat | grep -i "onnx\|model\|ir\|opset"

# Watch for specific messages
adb logcat | grep "Loaded ONNX model"
```

Checklist:
- [ ] App launches without crashing
- [ ] NO "IR version 13" messages ✅
- [ ] NO "Unsupported opset" messages ✅
- [ ] Model loading succeeds (if logged)

### Step 5: Test Functionality ✓

Launch the app and test:
- [ ] App starts normally
- [ ] Deep learning features load
- [ ] No model-related error messages
- [ ] Features that use models work correctly
- [ ] No crashes or exceptions
- [ ] Performance is acceptable

---

## Quick Deployment (TL;DR)

```bash
# 1. Copy models
cp ./generated-opset9-models/*.onnx <app-models-path>/

# 2. Build
./gradlew clean build

# 3. Deploy
adb install -r build/outputs/apk/release/app-release.apk

# 4. Verify
adb logcat | grep -i "ir\|onnx"
# Should NOT contain "IR version 13"
```

---

## Troubleshooting During Deployment

### "Build failed" / Compilation errors
- [ ] Models copied to correct path
- [ ] No syntax errors in resource files
- [ ] Run: `./gradlew clean sync gradle`
- [ ] Try again: `./gradlew build`

### "Install failed: signature mismatch"
- [ ] Make sure you're using same variant (debug/release)
- [ ] Uninstall old version first: `adb uninstall <package>`
- [ ] Then reinstall

### "Device not found" (ADB)
- [ ] Check: `adb devices`
- [ ] Enable USB debugging on device
- [ ] Restart adb: `adb kill-server && adb start-server`
- [ ] Try again

### "Still getting IR version 13 error"
- [ ] Clear app cache: `adb shell pm clear <package>`
- [ ] Uninstall completely: `adb uninstall <package>`
- [ ] Verify models in app dir: `<app-models-path>/*.onnx`
- [ ] Check file sizes (should be ~50KB)
- [ ] Reinstall fresh APK

### "Models not loading at all"
- [ ] Verify file paths in code match model location
- [ ] Check file permissions: `chmod 644 *.onnx`
- [ ] Ensure models in correct resource folder
- [ ] Check logcat for specific errors: `adb logcat -A`

---

## Success Indicators

When deployment is successful, you'll see:

✅ **Build**
- `BUILD SUCCESSFUL` in gradle output
- APK generated without errors

✅ **Installation**
- `Success` in adb install output
- App icon appears on device

✅ **Logs** (check with `adb logcat`)
- Absence of: `IR version 13`
- Absence of: `Unsupported opset`
- Presence of: Model loading messages (if logged)

✅ **Functionality**
- App runs normally
- No model-related crashes
- Deep learning features work
- Performance is good

---

## Rollback (If Needed)

If something goes wrong:

```bash
# 1. Restore backup models
cp models_backup/*.onnx <app-models-path>/

# 2. Rebuild
./gradlew clean build

# 3. Reinstall
adb uninstall <package>
adb install -r build/outputs/apk/release/app-release.apk

# 4. Verify
adb logcat | grep "model\|loaded"
```

---

## Next Steps After Successful Deployment

- [ ] Document where models are stored in your app
- [ ] Update team if applicable
- [ ] Monitor app performance (should be same as before)
- [ ] Report success to project
- [ ] Clean up temporary files if desired

---

## Support References

- **Error Details**: See `ONNX_FIX_SUMMARY.md`
- **Deployment Guide**: See `DEPLOYMENT_GUIDE.md`
- **Script Usage**: See `BUILD_MONITOR_README.md`
- **Quick Reference**: See `README_ONNX_FIX.txt`

---

## Status Check

- Models Generated: ✅ YES
- Models Downloaded: ✅ YES
- Documentation Ready: ✅ YES
- Ready to Deploy: ✅ YES

**Next Action**: Follow "Deployment Steps" above

---

**Generated**: 2025-01-13
**Status**: Ready for Deployment
**Models**: Opset 9 Compatible
**Location**: `./generated-opset9-models/`
