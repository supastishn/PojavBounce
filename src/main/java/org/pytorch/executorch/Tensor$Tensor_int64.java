/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import java.nio.Buffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_int64
extends Tensor {
    private final LongBuffer data;

    private Tensor.Tensor_int64(LongBuffer data, long[] shape) {
        super(shape, null);
        this.data = data;
    }

    @Override
    public DType dtype() {
        return DType.INT64;
    }

    @Override
    Buffer getRawDataBuffer() {
        return this.data;
    }

    @Override
    public long[] getDataAsLongArray() {
        this.data.rewind();
        long[] arr = new long[this.data.remaining()];
        this.data.get(arr);
        return arr;
    }

    public String toString() {
        return String.format("Tensor(%s, dtype=torch.int64)", Arrays.toString(this.shape));
    }
}
