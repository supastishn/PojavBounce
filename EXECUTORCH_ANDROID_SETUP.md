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

**Important:** The fbjni Java classes and native libraries (`libfbjni.so` and `libc++_shared.so`) are **already included in the mod** and will be automatically extracted. You **only** need to obtain `libexecutorch.so`.

The mod will automatically:
1. Extract `libc++_shared.so` (Android C++ standard library) from JAR resources
2. Extract `libfbjni.so` (Facebook JNI library, version 0.7.0) from JAR resources
3. Load these dependencies in the correct order

**You must provide:**
- `libexecutorch.so` - ExecuTorch runtime library built for your architecture

**Important:** If you previously manually placed `libfbjni.so` or `libc++_shared.so` in the native folder, the mod will now use the version-matched libraries from the JAR instead. The JAR-bundled libraries are guaranteed to match the Java classes version (fbjni 0.7.0). If you want to use manually placed libraries, remove them from the native folder first to let the mod extract the correct versions.

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

The mod automatically extracts `libfbjni.so` and `libc++_shared.so` from JAR resources. You only need to place `libexecutorch.so`.

1. Copy `libexecutorch.so` to your device
2. Place it in the ExecuTorch native folder:
   - Path: `<cache>/LiquidBounce/executorch/native/`
   - On PojavLauncher, this is typically: `/data/user/0/com.tungsten.fcl/cache/fclauncher/LiquidBounce/executorch/native/`
   - Required file: `libexecutorch.so`
   
**Note:** The mod will automatically extract and load `libfbjni.so` and `libc++_shared.so` from the JAR. If you have old versions of these files in the native folder from previous setups, you can delete them - the mod will use the version-matched libraries from the JAR.

The exact path will be shown in the error logs when the mod attempts to load the library.

### Step 4: Set Permissions

Ensure the library file has proper permissions:
```bash
chmod 755 /path/to/libexecutorch.so
```

**Note:** libfbjni.so permissions are automatically set when extracted by the mod. The mod now prioritizes JAR-extracted libraries over manually placed ones to ensure version compatibility.

### Step 5: Restart

Restart Minecraft. The mod should now load the ExecuTorch library successfully.

## Verification

Check the logs for these messages:

**Success:**
```
[ExecuTorch] Attempting to load libfbjni.so from JAR resources
[NativeLibraryExtractor] Attempting to extract libc++_shared.so for aarch64
[NativeLibraryExtractor] Found library at /native/libc++_shared.so
[NativeLibraryExtractor] Extracted library to /path/to/native/libc++_shared.so
[NativeLibraryExtractor] Attempting to extract libfbjni.so for aarch64
[NativeLibraryExtractor] Found library at /native/libfbjni.so
[NativeLibraryExtractor] Extracted library to /path/to/native/libfbjni.so
[ExecuTorch] Extracted libfbjni.so from JAR (version-matched with Java classes)
[ExecuTorch] Successfully loaded libfbjni.so from JAR
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
- **Symptom**: "Could not initialize class com.facebook.jni.ThreadScopeSupport", "UnsatisfiedLinkError", or similar errors
- **Root Cause**: Version mismatch between fbjni Java classes and native library, or failed library loading due to missing system dependencies
- **Solution**: 
  - The mod now automatically extracts version-matched `libfbjni.so` (0.7.0) from JAR resources
  - **Delete any manually placed** `libfbjni.so` or `libc++_shared.so` from the native folder to let the mod use the correct versions
  - Restart the application to allow fresh extraction
  - If the issue persists, check logs for the actual error (e.g., missing `libandroid.so` or `liblog.so` - these should be available on Android)
  - Ensure your `libexecutorch.so` was built against fbjni 0.7.0 headers

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
- This mod includes fbjni 0.7.0 (Java classes and native library bundled in JAR)
- The mod automatically extracts the version-matched `libfbjni.so` on first run
- ExecuTorch 1.0.1 or later is recommended for compatibility
- If you have manually placed `libfbjni.so` from a different source, remove it - the mod will use the bundled version
- Ensure your `libexecutorch.so` was built against fbjni 0.7.0 headers for compatibility

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

The mod loads libraries in this order:
1. **JAR extraction (preferred)**: Extract version-matched `libc++_shared.so` and `libfbjni.so` from JAR resources and load them
2. **Manual placement (fallback)**: If JAR extraction fails, try loading manually placed libraries from the native folder
3. **System library path (final fallback)**: Try `System.loadLibrary()` to use system-provided libraries

This order ensures version compatibility by prioritizing the bundled libraries that match the Java classes.

For `libexecutorch.so`:
1. **Manual placement**: Check for manually placed file in native folder and load it
2. **JAR extraction**: Try to extract from embedded resources (if available)
3. **System library path**: Try `System.loadLibrary("executorch")` as fallback

Dependencies are always loaded before their dependents (`libc++_shared.so` → `libfbjni.so` → `libexecutorch.so`).

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
