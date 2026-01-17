/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.facebook.jni.HybridData
 *  com.facebook.jni.annotations.DoNotStrip
 *  com.facebook.soloader.nativeloader.NativeLoader
 *  com.facebook.soloader.nativeloader.NativeLoaderDelegate
 *  com.facebook.soloader.nativeloader.SystemDelegate
 */
package org.pytorch.executorch.training;

import com.facebook.jni.HybridData;
import com.facebook.jni.annotations.DoNotStrip;
import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.NativeLoaderDelegate;
import com.facebook.soloader.nativeloader.SystemDelegate;
import java.util.Map;
import org.pytorch.executorch.Tensor;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
public class SGD {
    private final HybridData mHybridData;

    @DoNotStrip
    private static native HybridData initHybrid(Map<String, Tensor> var0, double var1, double var3, double var5, double var7, boolean var9);

    private SGD(Map<String, Tensor> namedParameters, double learningRate, double momentum, double dampening, double weightDecay, boolean nesterov) {
        this.mHybridData = SGD.initHybrid(namedParameters, learningRate, momentum, dampening, weightDecay, nesterov);
    }

    public static SGD create(Map<String, Tensor> namedParameters, double learningRate, double momentum, double dampening, double weightDecay, boolean nesterov) {
        return new SGD(namedParameters, learningRate, momentum, dampening, weightDecay, nesterov);
    }

    public static SGD create(Map<String, Tensor> namedParameters, double learningRate) {
        return SGD.create(namedParameters, learningRate, 0.0, 0.0, 0.0, false);
    }

    public void step(Map<String, Tensor> namedGradients) {
        if (!this.mHybridData.isValid()) {
            throw new RuntimeException("Attempt to use a destroyed SGD optimizer");
        }
        this.stepNative(namedGradients);
    }

    @DoNotStrip
    private native void stepNative(Map<String, Tensor> var1);

    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init((NativeLoaderDelegate)new SystemDelegate());
        }
        NativeLoader.loadLibrary((String)"executorch");
    }
}
