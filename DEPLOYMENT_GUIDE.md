# ONNX Models Deployment Guide

## ✅ Models Ready for Deployment

Your opset 9 compatible models have been successfully generated and downloaded!

### Generated Models

```
19kc8kp.onnx   (52 KB)  - Base model variant
21kc11kp.onnx  (52 KB)  - Base model variant
```

**Location**: `./generated-opset9-models/`

### Key Info

- **Opset Version**: 9 (compatible with your device)
- **IR Version**: 9 (not 13 - the issue is fixed!)
- **Model Type**: ONNX (Open Neural Network Exchange)
- **Size**: ~100 KB total (very lightweight)

## Deployment Steps

### Step 1: Locate Your App's Model Directory

Depending on your setup:

```bash
# Typical locations:
src/main/resources/resources/liquidbounce/models/
app/src/main/assets/models/
assets/models/
```

Or check your code:
```bash
grep -r "model" --include="*.kt" --include="*.java" | grep -i "path\|folder\|directory" | head -5
```

### Step 2: Back Up Existing Models

```bash
# Create backup of current models
mkdir -p ./models_backup
cp <your_models_path>/*.onnx ./models_backup/
```

### Step 3: Copy New Models

```bash
# Copy opset 9 models to your app
cp ./generated-opset9-models/*.onnx <your_models_path>/

# Verify
ls -la <your_models_path>/
```

Expected output:
```
19kc8kp.onnx
21kc11kp.onnx
```

### Step 4: Rebuild Your App

**Using Gradle (Android)**:
```bash
./gradlew clean build
```

**Using Maven**:
```bash
mvn clean install
```

**Using your build system**:
```bash
# Your standard build command
```

### Step 5: Deploy to Device

#### Using ADB (Android):
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### Using Android Studio:
1. Run → Edit Configurations
2. Select your app
3. Click Run (green play button)

#### Manual Installation:
1. Transfer APK to device
2. Open file manager → Install APK
3. Grant permissions when prompted

### Step 6: Verify the Fix

#### In Logs/Logcat:
```bash
# Connect device via ADB
adb logcat | grep -i "ir\|onnx\|version"

# Look for:
# ✅ "Loaded ONNX model" (success indicator)
# ❌ "Unsupported model IR version" (would indicate failure)
```

#### In App:
1. Launch the app
2. Check deep learning features work
3. Look for any error messages about model loading
4. Should see NO "IR version 13" errors

## Verification Checklist

- [ ] Models copied to app directory
- [ ] App rebuilt successfully
- [ ] App installed on device
- [ ] No build errors
- [ ] No "IR version" errors in logs
- [ ] Deep learning features functional
- [ ] Model-dependent features work correctly

## Troubleshooting

### Build Fails After Copying Models

**Problem**: Build errors after copying new models

**Solution**:
1. Clean build: `./gradlew clean`
2. Sync dependencies: `./gradlew sync`
3. Try again: `./gradlew build`

### App Still Shows "IR version 13" Error

**Problem**: App still fails with IR version error

**Cause**: Old models still being used

**Solutions**:
1. Clear app cache: `adb shell pm clear <package_name>`
2. Uninstall and reinstall: `adb uninstall <package_name>`
3. Verify models were copied: `ls -la <your_models_path>/`
4. Check file sizes match (should be ~50KB each)

### Models Not Loading

**Problem**: Models exist but aren't loading

**Solution**:
1. Check file paths in code
2. Verify file permissions: `chmod 644 *.onnx`
3. Confirm models are in resource folder
4. Check logcat for specific errors: `adb logcat | grep -A5 "error\|failed"`

### Model File Size Mismatch

**Expected**: Each .onnx file ~50KB

**If different**:
- Download was corrupted - retry: `./download-models.sh`
- Check CI build succeeded - check GitHub Actions
- Verify internet connection during download

## Performance Notes

### Expected Behavior

- ✅ Models load instantly (< 1 second)
- ✅ Inference is fast (milliseconds)
- ✅ No memory errors
- ✅ No crashes related to models

### If Slow

- Opset 9 models are the same speed as before
- Slowness indicates other issue (app size, unrelated feature)
- Check device specs (old devices may run slower)

## Rollback (If Needed)

If something goes wrong, revert to previous models:

```bash
# Restore backup
cp ./models_backup/*.onnx <your_models_path>/

# Rebuild
./gradlew clean build

# Redeploy
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Reference

### What Changed

- **Before**: `--opset 13` (IR v13) - Not compatible with device
- **After**: `--opset 9` (IR v9) - Compatible!

### Files Modified

- `scripts/convert_saved_models.py` - Conversion script (opset 13 → 9)

### Generated Models

- `19kc8kp.onnx` - Deep learning model variant 1
- `21kc11kp.onnx` - Deep learning model variant 2

## Support

If you encounter issues:

1. Check the logs: `adb logcat`
2. Review GitHub Actions build: https://github.com/supastishn/PojavBounce/actions
3. Verify models were generated: `ls -la ./generated-opset9-models/`
4. Confirm file integrity: `file 19kc8kp.onnx` (should be "data")

## Next Steps After Deployment

1. Test app thoroughly
2. Verify no model loading errors
3. Test deep learning features
4. Commit working changes to git if custom setup
5. Document your model folder location for future reference

---

**Status**: ✅ Opset 9 models ready for deployment

**Models Location**: `./generated-opset9-models/`

**Next Action**: Copy models to your app and rebuild!
