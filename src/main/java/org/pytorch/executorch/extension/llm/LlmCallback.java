/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.facebook.jni.annotations.DoNotStrip
 */
package org.pytorch.executorch.extension.llm;

import com.facebook.jni.annotations.DoNotStrip;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
public interface LlmCallback {
    @DoNotStrip
    public void onResult(String var1);

    @DoNotStrip
    default public void onStats(String stats) {
    }
}
