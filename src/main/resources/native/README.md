# Native Libraries

This directory contains native libraries for ExecuTorch support on Android.

## Current Libraries

- `libfbjni.so` (173 KB) - Facebook JNI library for aarch64
- `libexecutorch.so` (19 MB) - ExecuTorch runtime for aarch64

## Adding libc++_shared.so

The `libfbjni.so` library depends on `libc++_shared.so` (Android C++ standard library). While the code will attempt to load this from system paths, you can bundle it with the mod to ensure compatibility on all Android devices.

### How to obtain libc++_shared.so

1. **From Android NDK**:
   ```bash
   # For aarch64 (ARM64)
   cp $ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
      src/main/resources/native/libc++_shared.so
   ```

2. **From a device**:
   - Copy from `/system/lib64/libc++_shared.so` (for aarch64 devices)
   - Ensure it's compatible with the target Android API level

3. **Pre-built binaries**:
   - Download from Android NDK releases on GitHub
   - Ensure version compatibility with `libfbjni.so`

### Verification

After adding the library:
```bash
# Check the file is present
ls -lh src/main/resources/native/libc++_shared.so

# Verify architecture (should show "ARM aarch64")
file src/main/resources/native/libc++_shared.so

# Check it's built for Android
readelf -d src/main/resources/native/libc++_shared.so | head -20
```

### Build and Test

After adding the library:
1. Rebuild the mod: `./gradlew build`
2. Test on Android device (PojavLauncher)
3. Check logs for successful loading:
   ```
   [ExecuTorch] Successfully loaded libc++_shared.so from extracted JAR
   [ExecuTorch] Successfully loaded libfbjni.so from extracted JAR
   [ExecuTorch] Successfully loaded native ExecuTorch library
   ```

## Architecture Support

Currently, only **aarch64** (ARM64) libraries are provided. To support other architectures:
- Build or obtain libraries for the target architecture
- Name them with the same filenames (the extractor looks for exact names)
- The code will extract and use them based on detected architecture

## Notes

- These libraries are automatically extracted to the device at runtime
- Extraction happens to the ExecuTorch native folder: `<cache>/LiquidBounce/executorch/native/`
- Libraries are loaded in dependency order: `libc++_shared.so` → `libfbjni.so` → `libexecutorch.so`
- On failure, the system will attempt fallback to system library paths
