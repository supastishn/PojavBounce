/*
 * Stub implementation of Facebook's SystemDelegate class.
 * This provides API compatibility without requiring the actual Facebook SoLoader library.
 */
package com.facebook.soloader.nativeloader;

/**
 * System delegate for loading native libraries using System.loadLibrary.
 * This stub allows ExecuTorch Java code to compile without the actual Facebook SoLoader dependency.
 */
public class SystemDelegate implements NativeLoaderDelegate {
    @Override
    public boolean loadLibrary(String shortName, int flags) {
        try {
            System.loadLibrary(shortName);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    @Override
    public String getLibraryPath(String libName) {
        return null;
    }

    @Override
    public int getSoSourcesVersion() {
        return 0;
    }
}
