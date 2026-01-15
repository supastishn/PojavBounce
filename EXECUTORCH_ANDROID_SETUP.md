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

**Note:** As of the latest version, both `libfbjni.so` (native library) and the fbjni Java classes are now included in the mod. The native library will be automatically extracted from the mod resources for common architectures (aarch64, arm, x86_64, x86). You only need to obtain `libexecutorch.so`.

You need to obtain `libexecutorch.so` built for your architecture. The `libfbjni.so` dependency and its Java classes will be automatically extracted/loaded from the mod.

#### Required Libraries:
1. **fbjni Java classes** - Facebook JNI Java library (dependency of ExecuTorch) - **Now included in mod**
2. **libfbjni.so** - Facebook JNI native library (dependency of ExecuTorch) - **Now included in mod**
3. **libexecutorch.so** - ExecuTorch runtime library - **You must provide this**

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

**Note:** libexecutorch.so must be compatible with libfbjni.so version 0.7.0 (included in the mod).

### Step 3: Place the Library

**Note:** With the latest version, both the fbjni Java classes and `libfbjni.so` are automatically included in the mod. The native library will be extracted from the mod resources. You only need to place `libexecutorch.so`.

1. Copy `libexecutorch.so` to your device
2. Place it in the ExecuTorch native folder:
   - Path: `<cache>/LiquidBounce/executorch/native/`
   - On PojavLauncher, this is typically: `/data/user/0/com.tungsten.fcl/cache/fclauncher/LiquidBounce/executorch/native/`
   - Required file:
     - `libexecutorch.so` (main library)
   - Optional (automatically extracted if not present):
     - `libfbjni.so` (dependency, will be auto-extracted from mod resources)

The exact path will be shown in the error logs when the mod attempts to load the library.

### Step 4: Set Permissions

Ensure the library file has proper permissions:
```bash
chmod 755 /path/to/libexecutorch.so
# Only needed if you manually placed libfbjni.so:
chmod 755 /path/to/libfbjni.so
```

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
- **Symptom**: "dlopen failed: library 'libfbjni.so' not found" or "com/facebook/jni/HybridData$Destructor" errors in logs
- **Solution**: This should no longer occur as both the fbjni Java classes and libfbjni.so are now included in the mod. The Java classes are automatically loaded, and the native library will be automatically extracted. If it still occurs, check that the mod was built correctly with the fbjni dependency.

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
  - Compatible ExecuTorch version
  - Correct ABI (e.g., arm64-v8a for aarch64)
  - Compatible builds (libfbjni.so and libexecutorch.so must be from compatible versions)

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
