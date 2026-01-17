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
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init(new SystemDelegate());
        }
        NativeLoader.loadLibrary("executorch");
        sInstance = new ExecuTorchRuntime();
    }

    private ExecuTorchRuntime() {
    }

    public static ExecuTorchRuntime getRuntime() {
        return sInstance;
    }
}
