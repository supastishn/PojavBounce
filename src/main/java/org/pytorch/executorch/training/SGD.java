package org.pytorch.executorch.training;

import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.util.Map;
import org.pytorch.executorch.Tensor;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/training/SGD.class */
public class SGD {
    private final HybridData mHybridData;

    @DoNotStrip
    private static native HybridData initHybrid(Map<String, Tensor> map, double d, double d2, double d3, double d4, boolean z);

    @DoNotStrip
    private native void stepNative(Map<String, Tensor> map);

    static {
        // NOTE: Native library loading is handled by ExecuTorchEngine.kt
        // The library must be loaded AFTER NativeLoader is initialized with the correct delegate
        // Do not load the library here - it will be loaded by ExecuTorchEngine before SGD is used
    }

    private SGD(Map<String, Tensor> namedParameters, double learningRate, double momentum, double dampening, double weightDecay, boolean nesterov) {
        this.mHybridData = initHybrid(namedParameters, learningRate, momentum, dampening, weightDecay, nesterov);
    }

    public static SGD create(Map<String, Tensor> namedParameters, double learningRate, double momentum, double dampening, double weightDecay, boolean nesterov) {
        return new SGD(namedParameters, learningRate, momentum, dampening, weightDecay, nesterov);
    }

    public static SGD create(Map<String, Tensor> namedParameters, double learningRate) {
        return create(namedParameters, learningRate, 0.0d, 0.0d, 0.0d, false);
    }

    public void step(Map<String, Tensor> namedGradients) {
        if (!this.mHybridData.isValid()) {
            throw new RuntimeException("Attempt to use a destroyed SGD optimizer");
        }
        stepNative(namedGradients);
    }
}
