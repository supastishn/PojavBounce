/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_float64
extends Tensor {
    private final DoubleBuffer data;

    private Tensor.Tensor_float64(DoubleBuffer data, long[] shape) {
        super(shape, null);
        this.data = data;
    }

    @Override
    public DType dtype() {
        return DType.DOUBLE;
    }

    @Override
    Buffer getRawDataBuffer() {
        return this.data;
    }

    @Override
    public double[] getDataAsDoubleArray() {
        this.data.rewind();
        double[] arr = new double[this.data.remaining()];
        this.data.get(arr);
        return arr;
    }

    public String toString() {
        return String.format("Tensor(%s, dtype=torch.float64)", Arrays.toString(this.shape));
    }
}
