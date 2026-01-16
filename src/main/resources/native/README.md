# Native Libraries

This directory contains native libraries for ExecuTorch support on Android.

## Current Libraries

Libraries are organized by ABI under `native/<abi>/`:

- `arm64-v8a`
  - `libfbjni.so` (fbjni 0.7.0)
  - `libc++_shared.so` (from fbjni 0.7.0)
  - `libexecutorch.so` (ExecuTorch 1.0.1)
- `x86_64`
  - `libfbjni.so` (fbjni 0.7.0)
  - `libc++_shared.so` (from fbjni 0.7.0)
  - `libexecutorch.so` (ExecuTorch 1.0.1)
- `x86` / `armeabi-v7a`
  - `libfbjni.so` + `libc++_shared.so` (fbjni 0.7.0) for users who provide their own `libexecutorch.so`

## Updating libc++_shared.so

The bundled `libc++_shared.so` comes from the official fbjni 0.7.0 release. If you need to replace it (e.g., for a new ABI), obtain a matching version and place it in the corresponding ABI folder.

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

Arm64 and x86_64 libraries are bundled. To support additional architectures:
- Build or obtain libraries for the target architecture
- Place them in `native/<abi>/` using the same filenames
- The extractor will pick the library that matches the detected architecture

## Notes

- These libraries are automatically extracted to the device at runtime
- Extraction happens to the ExecuTorch native folder: `<cache>/LiquidBounce/executorch/native/`
- Libraries are loaded in dependency order: `libc++_shared.so` → `libfbjni.so` → `libexecutorch.so`
- On failure, the system will attempt fallback to system library paths
