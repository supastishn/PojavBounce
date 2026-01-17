/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_int32
extends Tensor {
    private final IntBuffer data;

    private Tensor.Tensor_int32(IntBuffer data, long[] shape) {
        super(shape, null);
        this.data = data;
    }

    @Override
    public DType dtype() {
        return DType.INT32;
    }

    @Override
    Buffer getRawDataBuffer() {
        return this.data;
    }

    @Override
    public int[] getDataAsIntArray() {
        this.data.rewind();
        int[] arr = new int[this.data.remaining()];
        this.data.get(arr);
        return arr;
    }

    public String toString() {
        return String.format("Tensor(%s, dtype=torch.int32)", Arrays.toString(this.shape));
    }
}
