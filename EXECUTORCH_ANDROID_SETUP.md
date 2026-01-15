# ExecuTorch Android Native Library Setup

## Overview

ExecuTorch requires native libraries to function on Android. Due to the unique environment of PojavLauncher, these libraries cannot be automatically bundled and must be manually provided.

## Why Manual Setup is Needed

PojavLauncher runs Minecraft Java Edition on Android using a custom JVM environment. This differs from standard Android apps in several ways:

1. **No Android SDK**: The standard `executorch-android` Maven dependency requires Android SDK components that aren't available in PojavLauncher
2. **Custom Library Paths**: Native libraries must be placed in specific locations accessible to the JVM
3. **Architecture Compatibility**: The library must match your device's CPU architecture (usually ARM64/aarch64)

## Setup Instructions

### Step 1: Determine Your Architecture

The mod will automatically detect your architecture and report it in the logs. Common architectures:
- **aarch64** (ARM64) - Most modern Android devices
- **arm** (ARM32) - Older Android devices
- **x86_64** - Android emulators and x86 devices
- **x86** - Older emulators

### Step 2: Obtain the Native Libraries

**Note:** As of the latest version, the fbjni Java classes, `libfbjni.so`, and `libc++_shared.so` are included in the mod and will be automatically extracted from resources if present. You primarily need to obtain `libexecutorch.so`.

You need to obtain `libexecutorch.so` built for your architecture. The mod will attempt to automatically load dependencies (`libc++_shared.so` and `libfbjni.so`) from:
1. JAR resources (if bundled)
2. Native folder (if manually placed)
3. System library paths (as fallback)

#### Required Libraries:
1. **fbjni Java classes** - Facebook JNI Java library (dependency of ExecuTorch) - **Included in mod**
2. **libfbjni.so** - Facebook JNI native library (dependency of ExecuTorch) - **Included in mod**
3. **libc++_shared.so** - Android C++ standard library (dependency of libfbjni.so) - **Can be bundled/auto-extracted**
4. **libexecutorch.so** - ExecuTorch runtime library - **You must provide this**

#### Option A: Build from Source (Recommended)
1. Follow the [ExecuTorch build instructions](https://pytorch.org/executorch/stable/build-run-coreml.html)
2. Build for Android using the Android NDK
3. Target your device's architecture (e.g., arm64-v8a for aarch64)

Example build command:
```bash
cmake -DCMAKE_INSTALL_PREFIX=executorch \
      -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-23 \
      -Bbuild .
cmake --build build --target install
```

#### Option B: Use Pre-built Binaries (If Available)
Some community members may provide pre-built binaries. Ensure they are:
- Built for your architecture
- Compatible with ExecuTorch version 1.0.1 or later
- Trusted source (security risk!)

**Note:** libexecutorch.so must be compatible with fbjni 0.7.0 (included in the mod). ExecuTorch 1.0.1 or later is recommended.

### Step 3: Place the Library

**Note:** With the latest version, the fbjni Java classes, `libfbjni.so`, and `libc++_shared.so` (if bundled) are automatically included in the mod. The native libraries will be extracted from the mod resources. You only need to place `libexecutorch.so`.

**Optional:** If you encounter loading issues, you can also manually place `libc++_shared.so` for your architecture in the native folder to ensure compatibility.

1. Copy `libexecutorch.so` to your device
2. Place it in the ExecuTorch native folder:
   - Path: `<cache>/LiquidBounce/executorch/native/`
   - On PojavLauncher, this is typically: `/data/user/0/com.tungsten.fcl/cache/fclauncher/LiquidBounce/executorch/native/`
   - Required file:
     - `libexecutorch.so` (main library)
   - Optional (will be auto-extracted from mod resources if present):
     - `libfbjni.so` (dependency)
     - `libc++_shared.so` (C++ standard library, dependency of libfbjni.so)

The exact path will be shown in the error logs when the mod attempts to load the library.

### Step 4: Set Permissions

Ensure the library file has proper permissions:
```bash
chmod 755 /path/to/libexecutorch.so
```

Note: libfbjni.so permissions are automatically set when extracted by the mod.

### Step 5: Restart

Restart Minecraft. The mod should now load the ExecuTorch library successfully.

## Verification

Check the logs for these messages:

**Success:**
```
[ExecuTorch] Attempting to extract native libraries for architecture: aarch64
[NativeLibraryExtractor] Found library at /native/android/aarch64/libfbjni.so
[NativeLibraryExtractor] Extracted library to /path/to/native/libfbjni.so
[ExecuTorch] Loading extracted library: /path/to/native/libfbjni.so
[ExecuTorch] Successfully loaded libfbjni.so
[ExecuTorch] Found manually placed ExecuTorch library at: /path/to/libexecutorch.so
[ExecuTorch] Successfully loaded native ExecuTorch library from manual placement
[ExecuTorch] ExecuTorch runtime initialized successfully
```

**Failure:**
```
[ExecuTorch] No native library found (checked manual and JAR extraction)
[ExecuTorch] Failed to load ExecuTorch native library
```

## Troubleshooting

### Library Not Found
- **Symptom**: "No native library found" in logs
- **Solution**: Verify the file paths and filenames (must be exactly `libfbjni.so` and `libexecutorch.so`)

### Missing Dependency
- **Symptom**: "Could not initialize class com.facebook.jni.ThreadScopeSupport", "dlopen failed: library not found", or similar errors
- **Solution**: 
  - The mod now includes fbjni Java classes, `libfbjni.so`, and supports bundling `libc++_shared.so`
  - If bundled in JAR resources, these will be auto-extracted
  - If you encounter this error, try manually placing `libc++_shared.so` for your architecture (aarch64) in the native folder
  - Get `libc++_shared.so` from Android NDK: `$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so`
  - Or place it in `src/main/resources/native/libc++_shared.so` to bundle it with the mod

### Wrong Architecture
- **Symptom**: "Failed to load native library" or crash
- **Solution**: Rebuild for the correct architecture (check logs for "detected arch:")

### Permission Denied
- **Symptom**: "Permission denied" in logs
- **Solution**: Run `chmod 755` on both library files

### Incompatible Library
- **Symptom**: "undefined symbol" or similar errors
- **Solution**: Ensure both libraries are built with:
  - Android NDK (not desktop GLIBC)
  - Compatible ExecuTorch version (1.0.1 or later recommended)
  - Correct ABI (e.g., arm64-v8a for aarch64)
  - Compatible builds (libfbjni.so 0.7.0 included in mod is compatible with most ExecuTorch 1.0+ builds)
  
**Version Compatibility Notes:**
- This mod includes fbjni 0.7.0 (Java classes and native library)
- ExecuTorch 1.0.1 or later is recommended
- If you encounter compatibility issues, ensure your libexecutorch.so was built against fbjni 0.7.0 headers
- For building ExecuTorch with fbjni 0.7.0, use the matching version of fbjni headers from Maven Central

## Alternative: Disable ExecuTorch

If you cannot set up the native library, ExecuTorch will gracefully disable itself and the mod will continue to work using the DJL backend (on desktop) or without deep learning features (on Android).

No action is required - the mod will log a warning and continue running.

## Technical Details

### Why Not Include in JAR?

The `executorch-android` Maven artifact requires:
- androidx.core:core-ktx
- Other Android SDK dependencies

These are not available in PojavLauncher's JVM environment, making it impossible to bundle the standard Android distribution.

### Library Loading Order

The mod attempts to load libraries in this order:
1. **Manual placement**: Check for `libfbjni.so` and `libexecutorch.so` in native folder
2. **JAR extraction**: Try to extract from embedded resources
3. **System library path**: Try `System.loadLibrary("fbjni")` and `System.loadLibrary("executorch")`

Dependencies are always loaded first (libfbjni.so before libexecutorch.so).

For Android/PojavLauncher, only manual placement (#1) will work.

## Community Resources

- [ExecuTorch Documentation](https://pytorch.org/executorch/)
- [Android NDK](https://developer.android.com/ndk)
- [PojavLauncher Discord](https://discord.gg/6RpEJda) - May have community builds

## Contributing

If you successfully build ExecuTorch for Android/PojavLauncher, please consider:
1. Documenting your build process
2. Sharing build scripts
3. (Optional) Providing trusted pre-built binaries

This will help other users set up ExecuTorch more easily.
