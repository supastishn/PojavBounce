package org.pytorch.executorch;

import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;

/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/ExecuTorchRuntime.class */
public class ExecuTorchRuntime {
    private static final ExecuTorchRuntime sInstance;

    @DoNotStrip
    public static native String[] getRegisteredOps();

    @DoNotStrip
    public static native String[] getRegisteredBackends();

    static {
        // NOTE: Native library loading is handled by ExecuTorchEngine.kt
        // The library must be loaded AFTER NativeLoader is initialized with the correct delegate
        // Do not load the library here - it will be loaded by ExecuTorchEngine before Runtime is used
        sInstance = new ExecuTorchRuntime();
    }

    private ExecuTorchRuntime() {
    }

    public static ExecuTorchRuntime getRuntime() {
        return sInstance;
    }
}
