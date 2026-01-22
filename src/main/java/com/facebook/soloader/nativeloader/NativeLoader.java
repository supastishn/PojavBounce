/*
 * Stub implementation of Facebook's NativeLoader class.
 * This provides API compatibility without requiring the actual Facebook SoLoader library.
 */
package com.facebook.soloader.nativeloader;

/**
 * Native library loader that delegates to a NativeLoaderDelegate.
 * This stub allows ExecuTorch Java code to compile without the actual Facebook SoLoader dependency.
 */
public class NativeLoader {
    private static NativeLoaderDelegate delegate = null;
    private static boolean initialized = false;

    /**
     * Initialize the NativeLoader with a delegate.
     * @param delegate The delegate to use for loading libraries
     */
    public static void init(NativeLoaderDelegate delegate) {
        if (initialized) {
            return;
        }
        NativeLoader.delegate = delegate;
        initialized = true;
    }

    /**
     * Load a native library by its short name.
     * @param shortName The library name without "lib" prefix and ".so" suffix
     * @return true if the library was loaded successfully
     */
    public static boolean loadLibrary(String shortName) {
        if (delegate != null) {
            return delegate.loadLibrary(shortName, 0);
        }
        // Fallback to System.loadLibrary if no delegate
        try {
            System.loadLibrary(shortName);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Check if the loader has been initialized.
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
