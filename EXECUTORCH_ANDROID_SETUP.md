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

### Step 2: Obtain the Native Library

You need to obtain `libexecutorch.so` built for your architecture. Options:

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

### Step 3: Place the Library

1. Copy `libexecutorch.so` to your device
2. Place it in the ExecuTorch native folder:
   - Path: `<cache>/LiquidBounce/executorch/native/libexecutorch.so`
   - On PojavLauncher, this is typically: `/data/user/0/com.tungsten.fcl/cache/fclauncher/LiquidBounce/executorch/native/libexecutorch.so`

The exact path will be shown in the error logs when the mod attempts to load the library.

### Step 4: Set Permissions

Ensure the library file has proper permissions:
```bash
chmod 755 /path/to/libexecutorch.so
```

### Step 5: Restart

Restart Minecraft. The mod should now load the ExecuTorch library successfully.

## Verification

Check the logs for these messages:

**Success:**
```
[ExecuTorch] Found manually placed library at: /path/to/libexecutorch.so
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
- **Solution**: Verify the file path and filename (must be exactly `libexecutorch.so`)

### Wrong Architecture
- **Symptom**: "Failed to load native library" or crash
- **Solution**: Rebuild for the correct architecture (check logs for "detected arch:")

### Permission Denied
- **Symptom**: "Permission denied" in logs
- **Solution**: Run `chmod 755` on the library file

### Incompatible Library
- **Symptom**: "undefined symbol" or similar errors
- **Solution**: Ensure library is built with:
  - Android NDK (not desktop GLIBC)
  - Compatible ExecuTorch version
  - Correct ABI (e.g., arm64-v8a for aarch64)

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
1. **Manual placement**: Check for `libexecutorch.so` in native folder
2. **JAR extraction**: Try to extract from embedded resources
3. **System library path**: Try `System.loadLibrary("executorch")`

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
