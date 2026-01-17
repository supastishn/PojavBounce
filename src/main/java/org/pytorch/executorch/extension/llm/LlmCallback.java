package org.pytorch.executorch.extension.llm;

import com.facebook.jni.annotations.DoNotStrip;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/extension/llm/LlmCallback.class */
public interface LlmCallback {
    @DoNotStrip
    void onResult(String str);

    @DoNotStrip
    default void onStats(String stats) {
    }
}
