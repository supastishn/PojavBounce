/*
 * Stub implementation of Facebook's HybridData class.
 * This provides API compatibility without requiring the actual Facebook JNI library.
 */
package com.facebook.jni;

/**
 * HybridData holds a reference to a C++ object.
 * This stub allows ExecuTorch Java code to compile without the actual Facebook JNI dependency.
 */
public class HybridData {
    private long mNativePointer;

    public HybridData() {
        this.mNativePointer = 0;
    }

    public void resetNative() {
        mNativePointer = 0;
    }

    public boolean isValid() {
        return mNativePointer != 0;
    }
}
