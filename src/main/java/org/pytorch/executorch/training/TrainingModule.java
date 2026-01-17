package org.pytorch.executorch.training;


import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Tensor;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/training/TrainingModule.class */
public class TrainingModule {
    private final HybridData mHybridData;

    @DoNotStrip
    private static native HybridData initHybrid(String str, String str2);

    @DoNotStrip
    private native EValue[] executeForwardBackwardNative(String str, EValue... eValueArr);

    @DoNotStrip
    private native Map<String, Tensor> namedParametersNative(String str);

    @DoNotStrip
    private native Map<String, Tensor> namedGradientsNative(String str);

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init(new SystemDelegate());
        }
        NativeLoader.loadLibrary("executorch");
    }

    private TrainingModule(String moduleAbsolutePath, String dataAbsolutePath) {
        this.mHybridData = initHybrid(moduleAbsolutePath, dataAbsolutePath);
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

    public EValue[] executeForwardBackward(String methodName, EValue... inputs) {
        if (!this.mHybridData.isValid()) {
            // Log.e("ExecuTorch", "Attempt to use a destroyed module");
            return new EValue[0];
        }
        return executeForwardBackwardNative(methodName, inputs);
    }

    public Map<String, Tensor> namedParameters(String methodName) {
        if (!this.mHybridData.isValid()) {
            // Log.e("ExecuTorch", "Attempt to use a destroyed module");
            return new HashMap();
        }
        return namedParametersNative(methodName);
    }

    public Map<String, Tensor> namedGradients(String methodName) {
        if (!this.mHybridData.isValid()) {
            // Log.e("ExecuTorch", "Attempt to use a destroyed module");
            return new HashMap();
        }
        return namedGradientsNative(methodName);
    }
}
