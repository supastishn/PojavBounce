/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_float32
extends Tensor {
    private final FloatBuffer data;

    Tensor.Tensor_float32(FloatBuffer data, long[] shape) {
        super(shape, null);
        this.data = data;
    }

    @Override
    public float[] getDataAsFloatArray() {
        this.data.rewind();
        float[] arr = new float[this.data.remaining()];
        this.data.get(arr);
        return arr;
    }

    @Override
    public DType dtype() {
        return DType.FLOAT;
    }

    @Override
    Buffer getRawDataBuffer() {
        return this.data;
    }

    public String toString() {
        return String.format("Tensor(%s, dtype=torch.float32)", Arrays.toString(this.shape));
    }
}
