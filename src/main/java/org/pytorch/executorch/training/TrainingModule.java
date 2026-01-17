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
package org.pytorch.executorch.training;

import android.util.Log;
import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.NativeLoaderDelegate;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Tensor;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
public class TrainingModule {
    private final HybridData mHybridData;

    @DoNotStrip
    private static native HybridData initHybrid(String var0, String var1);

    private TrainingModule(String moduleAbsolutePath, String dataAbsolutePath) {
        this.mHybridData = TrainingModule.initHybrid(moduleAbsolutePath, dataAbsolutePath);
    }

    public static TrainingModule load(String modelPath, String dataPath) {
        File modelFile = new File(modelPath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path!! " + modelPath);
        }
        File dataFile = new File(dataPath);
        if (!dataFile.canRead() || !dataFile.isFile()) {
            throw new RuntimeException("Cannot load data path!! " + dataPath);
        }
        return new TrainingModule(modelPath, dataPath);
    }

    public static TrainingModule load(String modelPath) {
        File modelFile = new File(modelPath);
        if (!modelFile.canRead() || !modelFile.isFile()) {
            throw new RuntimeException("Cannot load model path!! " + modelPath);
        }
        return new TrainingModule(modelPath, "");
    }

    public EValue[] executeForwardBackward(String methodName, EValue ... inputs) {
        if (!this.mHybridData.isValid()) {
            Log.e((String)"ExecuTorch", (String)"Attempt to use a destroyed module");
            return new EValue[0];
        }
        return this.executeForwardBackwardNative(methodName, inputs);
    }

    @DoNotStrip
    private native EValue[] executeForwardBackwardNative(String var1, EValue ... var2);

    public Map<String, Tensor> namedParameters(String methodName) {
        if (!this.mHybridData.isValid()) {
            Log.e((String)"ExecuTorch", (String)"Attempt to use a destroyed module");
            return new HashMap<String, Tensor>();
        }
        return this.namedParametersNative(methodName);
    }

    @DoNotStrip
    private native Map<String, Tensor> namedParametersNative(String var1);

    public Map<String, Tensor> namedGradients(String methodName) {
        if (!this.mHybridData.isValid()) {
            Log.e((String)"ExecuTorch", (String)"Attempt to use a destroyed module");
            return new HashMap<String, Tensor>();
        }
        return this.namedGradientsNative(methodName);
    }

    @DoNotStrip
    private native Map<String, Tensor> namedGradientsNative(String var1);

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init((NativeLoaderDelegate)new SystemDelegate());
        }
        NativeLoader.loadLibrary((String)"executorch");
    }
}
