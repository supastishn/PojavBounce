package org.pytorch.executorch;


import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/Module.class */
public class Module {
    public static final int LOAD_MODE_FILE = 0;
    public static final int LOAD_MODE_MMAP = 1;
    public static final int LOAD_MODE_MMAP_USE_MLOCK = 2;
    public static final int LOAD_MODE_MMAP_USE_MLOCK_IGNORE_ERRORS = 3;
    private final HybridData mHybridData;
    private final Map<String, MethodMetadata> mMethodMetadata;
    private Lock mLock = new ReentrantLock();

    @DoNotStrip
    private static native HybridData initHybrid(String str, int i, int i2);

    @DoNotStrip
    private native EValue[] executeNative(String str, EValue... eValueArr);

    @DoNotStrip
    private native int loadMethodNative(String str);

    @DoNotStrip
    private native String[] getUsedBackends(String str);

    @DoNotStrip
    public native String[] getMethods();

    @DoNotStrip
    private static native String[] readLogBufferStaticNative();

    @DoNotStrip
    private native String[] readLogBufferNative();

    @DoNotStrip
    @Experimental
    public native boolean etdump();

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init(new SystemDelegate());
        }
        NativeLoader.loadLibrary("executorch");
    }

    private Module(String moduleAbsolutePath, int loadMode, int numThreads) {
        ExecuTorchRuntime.getRuntime();
        this.mHybridData = initHybrid(moduleAbsolutePath, loadMode, numThreads);
        this.mMethodMetadata = populateMethodMeta();
    }

    Map<String, MethodMetadata> populateMethodMeta() {
        String[] methods = getMethods();
        Map<String, MethodMetadata> metadata = new HashMap<>();
        for (String name : methods) {
            metadata.put(name, new MethodMetadata().setName(name));
        }
        return metadata;
    }

    public static Module load(String modelPath, int loadMode) {
        return load(modelPath, loadMode, 0);
    }

    public static Module load(String modelPath, int loadMode, int numThreads) {
        File modelFile = new File(modelPath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path " + modelPath);
        }
        return new Module(modelPath, loadMode, numThreads);
    }

    public static Module load(String modelPath) {
        return load(modelPath, 0);
    }

    public EValue[] forward(EValue... inputs) {
        return execute("forward", inputs);
    }

    public EValue[] execute(String methodName, EValue... inputs) {
        try {
            this.mLock.lock();
            if (!this.mHybridData.isValid()) {
                // Log.e("ExecuTorch", "Attempt to use a destroyed module");
                EValue[] eValueArr = new EValue[0];
                this.mLock.unlock();
                return eValueArr;
            }
            EValue[] executeNative = executeNative(methodName, inputs);
            this.mLock.unlock();
            return executeNative;
        } catch (Throwable th) {
            this.mLock.unlock();
            throw th;
        }
    }

    public int loadMethod(String methodName) {
        try {
            this.mLock.lock();
            if (!this.mHybridData.isValid()) {
                // Log.e("ExecuTorch", "Attempt to use a destroyed module");
                return 2;
            }
            return loadMethodNative(methodName);
        } finally {
            this.mLock.unlock();
        }
    }

    public MethodMetadata getMethodMetadata(String name) {
        if (!this.mMethodMetadata.containsKey(name)) {
            throw new RuntimeException("method " + name + "does not exist for this module");
        }
        return this.mMethodMetadata.get(name);
    }

    public static String[] readLogBufferStatic() {
        return readLogBufferStaticNative();
    }

    public String[] readLogBuffer() {
        return readLogBufferNative();
    }

    public void destroy() {
        if (this.mLock.tryLock()) {
            try {
                this.mHybridData.resetNative();
                return;
            } finally {
                this.mLock.unlock();
            }
        }
        // Log.w("ExecuTorch", "Destroy was called while the module was in use. Resources will not be immediately released.");
    }
}
