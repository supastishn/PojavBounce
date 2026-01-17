/*
 * Stub implementation of org.pytorch.executorch.Module
 * This provides the Java class structure that libexecutorch.so expects
 *
 * The real implementation is in executorch-android AAR which is incompatible with PojavLauncher
 * This stub allows the native library to load without errors
 */
package org.pytorch.executorch;

import com.facebook.jni.HybridData;

public class Module {
    // HybridData is the bridge between Java and native code in fbjni
    private final HybridData mHybridData;

    // Native method declarations that libexecutorch.so will provide
    private static native HybridData initHybrid(String modelPath);
    private native Object forward(Object... inputs);
    private native void destroy();

    /**
     * Load an ExecuTorch model from a file path
     */
    public Module(String modelPath) {
        this.mHybridData = initHybrid(modelPath);
    }

    /**
     * Execute the model with given inputs
     */
    public Object execute(Object... inputs) {
        return forward(inputs);
    }

    /**
     * Clean up native resources
     */
    public void close() {
        destroy();
    }

    /**
     * Reset native peer - required by HybridData
     */
    protected void resetNative() {
        mHybridData.resetNative();
    }
}
