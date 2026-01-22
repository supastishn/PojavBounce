/*
 * Stub implementation of Facebook's NativeLoaderDelegate interface.
 * This provides API compatibility without requiring the actual Facebook SoLoader library.
 */
package com.facebook.soloader.nativeloader

/**
 * Delegate interface for native library loading.
 * This stub allows ExecuTorchEngine to compile without the actual Facebook SoLoader dependency.
 */
interface NativeLoaderDelegate {
    /**
     * Load a native library by its short name.
     * @param shortName The library name without "lib" prefix and ".so" suffix
     * @param flags Loading flags (implementation-specific)
     * @return true if the library was loaded successfully
     */
    fun loadLibrary(shortName: String, flags: Int): Boolean

    /**
     * Get the path to a native library.
     * @param libName The library name
     * @return The path to the library, or null if not found
     */
    fun getLibraryPath(libName: String): String?

    /**
     * Get the version of the SO sources.
     * @return The version number
     */
    fun getSoSourcesVersion(): Int
}
