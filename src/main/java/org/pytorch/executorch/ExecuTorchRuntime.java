/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.facebook.jni.annotations.DoNotStrip
 *  com.facebook.soloader.nativeloader.NativeLoader
 *  com.facebook.soloader.nativeloader.NativeLoaderDelegate
 *  com.facebook.soloader.nativeloader.SystemDelegate
 */
package org.pytorch.executorch;

import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.NativeLoaderDelegate;
import com.facebook.soloader.nativeloader.SystemDelegate;

public class ExecuTorchRuntime {
    private static final ExecuTorchRuntime sInstance;

    private ExecuTorchRuntime() {
    }

    public static ExecuTorchRuntime getRuntime() {
        return sInstance;
    }

    @DoNotStrip
    public static native String[] getRegisteredOps();

    @DoNotStrip
    public static native String[] getRegisteredBackends();

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init((NativeLoaderDelegate)new SystemDelegate());
        }
        NativeLoader.loadLibrary((String)"executorch");
        sInstance = new ExecuTorchRuntime();
    }
}
