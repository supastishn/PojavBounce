/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.Log
 */
package org.pytorch.executorch;

import android.util.Log;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_unsupported
extends Tensor {
    private final ByteBuffer data;
    private final DType mDtype;

    private Tensor.Tensor_unsupported(ByteBuffer data, long[] shape, DType dtype) {
        super(shape, null);
        this.data = data;
        this.mDtype = dtype;
        Log.e((String)"ExecuTorch", (String)(this.toString() + " in Java. Please consider re-export the model with proper return type"));
    }

    @Override
    public DType dtype() {
        return this.mDtype;
    }

    public String toString() {
        return String.format("Unsupported tensor(%s, dtype=%d)", new Object[]{Arrays.toString(this.shape), this.mDtype});
    }
}
