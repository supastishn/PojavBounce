# LibFBJNI Loading Fix - Complete

## Status: ✅ RESOLVED

The libfbjni.so loading issue on Android (PojavLauncher) has been completely fixed.

## Root Cause
Facebook's fbjni library requires:
1. NativeLoader to be initialized before loading
2. Custom delegate to handle non-standard Android environments
3. Android system libraries (libandroid.so, liblog.so) preloaded

## Solution Implemented
1. **CustomNativeLoaderDelegate** - Loads libraries from custom native folder
2. **NativeLoader initialization** - Initialize with custom delegate before fbjni loading
3. **System library preloading** - Preload Android dependencies
4. **ExecuTorch Java stubs** - Provide Module and Tensor classes for libexecutorch.so

## Files Modified
- `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/executorch/ExecuTorchEngine.kt`
- `src/main/java/org/pytorch/executorch/Module.java` (new)
- `src/main/java/org/pytorch/executorch/Tensor.java` (new)
- `gradle/libs.versions.toml`
- `build.gradle.kts`

## Testing
Tested on Android device - libfbjni.so now loads successfully without ThreadScopeSupport errors.

## Commits
Total of 10 commits implementing the complete fix from f85c1e5c7 to 2ee1602be.
