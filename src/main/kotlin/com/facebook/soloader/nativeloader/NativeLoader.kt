/*
 * Stub implementation of Facebook's NativeLoader class.
 * This provides API compatibility without requiring the actual Facebook SoLoader library.
 */
package com.facebook.soloader.nativeloader

/**
 * Native library loader that delegates to a [NativeLoaderDelegate].
 * This stub allows ExecuTorchEngine to compile without the actual Facebook SoLoader dependency.
 */
object NativeLoader {
    private var delegate: NativeLoaderDelegate? = null
    private var initialized = false

    /**
     * Initialize the NativeLoader with a delegate.
     * @param delegate The delegate to use for loading libraries
     */
    @JvmStatic
    fun init(delegate: NativeLoaderDelegate) {
        if (initialized) {
            return
        }
        this.delegate = delegate
        initialized = true
    }

    /**
     * Load a native library by its short name.
     * @param shortName The library name without "lib" prefix and ".so" suffix
     * @return true if the library was loaded successfully
     * @throws IllegalStateException if the loader has not been initialized
     */
    @JvmStatic
    fun loadLibrary(shortName: String): Boolean {
        val del = delegate
        if (del != null) {
            return del.loadLibrary(shortName, 0)
        }
        // Fallback to System.loadLibrary if no delegate
        return try {
            System.loadLibrary(shortName)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Check if the loader has been initialized.
     * @return true if initialized
     */
    @JvmStatic
    fun isInitialized(): Boolean = initialized
}
