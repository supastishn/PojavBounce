/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.Log
 *  com.facebook.jni.HybridData
 *  com.facebook.jni.annotations.DoNotStrip
 *  com.facebook.soloader.nativeloader.NativeLoader
 *  com.facebook.soloader.nativeloader.NativeLoaderDelegate
 *  com.facebook.soloader.nativeloader.SystemDelegate
 */
package org.pytorch.executorch;

import android.util.Log;
import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.NativeLoaderDelegate;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.pytorch.executorch.EValue;
import org.pytorch.executorch.ExecuTorchRuntime;
import org.pytorch.executorch.MethodMetadata;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
public class Module {
    public static final int LOAD_MODE_FILE = 0;
    public static final int LOAD_MODE_MMAP = 1;
    public static final int LOAD_MODE_MMAP_USE_MLOCK = 2;
    public static final int LOAD_MODE_MMAP_USE_MLOCK_IGNORE_ERRORS = 3;
    private final HybridData mHybridData;
    private final Map<String, MethodMetadata> mMethodMetadata;
    private Lock mLock = new ReentrantLock();

    @DoNotStrip
    private static native HybridData initHybrid(String var0, int var1, int var2);

    private Module(String moduleAbsolutePath, int loadMode, int numThreads) {
        ExecuTorchRuntime runtime = ExecuTorchRuntime.getRuntime();
        this.mHybridData = Module.initHybrid(moduleAbsolutePath, loadMode, numThreads);
        this.mMethodMetadata = this.populateMethodMeta();
    }

    Map<String, MethodMetadata> populateMethodMeta() {
        String[] methods = this.getMethods();
        HashMap<String, MethodMetadata> metadata = new HashMap<String, MethodMetadata>();
        for (int i = 0; i < methods.length; ++i) {
            String name = methods[i];
            metadata.put(name, new MethodMetadata().setName(name));
        }
        return metadata;
    }

    public static Module load(String modelPath, int loadMode) {
        return Module.load(modelPath, loadMode, 0);
    }

    public static Module load(String modelPath, int loadMode, int numThreads) {
        File modelFile = new File(modelPath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path " + modelPath);
        }
        return new Module(modelPath, loadMode, numThreads);
    }

    public static Module load(String modelPath) {
        return Module.load(modelPath, 0);
    }

    public EValue[] forward(EValue ... inputs) {
        return this.execute("forward", inputs);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public EValue[] execute(String methodName, EValue ... inputs) {
        try {
            this.mLock.lock();
            if (!this.mHybridData.isValid()) {
                Log.e((String)"ExecuTorch", (String)"Attempt to use a destroyed module");
                EValue[] eValueArray = new EValue[]{};
                return eValueArray;
            }
            EValue[] eValueArray = this.executeNative(methodName, inputs);
            return eValueArray;
        }
        finally {
            this.mLock.unlock();
        }
    }

    @DoNotStrip
    private native EValue[] executeNative(String var1, EValue ... var2);

    public int loadMethod(String methodName) {
        try {
            this.mLock.lock();
            if (!this.mHybridData.isValid()) {
                Log.e((String)"ExecuTorch", (String)"Attempt to use a destroyed module");
                int n = 2;
                return n;
            }
            int n = this.loadMethodNative(methodName);
            return n;
        }
        finally {
            this.mLock.unlock();
        }
    }

    @DoNotStrip
    private native int loadMethodNative(String var1);

    @DoNotStrip
    private native String[] getUsedBackends(String var1);

    @DoNotStrip
    public native String[] getMethods();

    public MethodMetadata getMethodMetadata(String name) {
        if (!this.mMethodMetadata.containsKey(name)) {
            throw new RuntimeException("method " + name + "does not exist for this module");
        }
        return this.mMethodMetadata.get(name);
    }

    @DoNotStrip
    private static native String[] readLogBufferStaticNative();

    public static String[] readLogBufferStatic() {
        return Module.readLogBufferStaticNative();
    }

    public String[] readLogBuffer() {
        return this.readLogBufferNative();
    }

    @DoNotStrip
    private native String[] readLogBufferNative();

    @Experimental
    @DoNotStrip
    public native boolean etdump();

    public void destroy() {
        if (this.mLock.tryLock()) {
            try {
                this.mHybridData.resetNative();
            }
            finally {
                this.mLock.unlock();
            }
        } else {
            Log.w((String)"ExecuTorch", (String)"Destroy was called while the module was in use. Resources will not be immediately released.");
        }
    }

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init((NativeLoaderDelegate)new SystemDelegate());
        }
        NativeLoader.loadLibrary((String)"executorch");
    }
}
