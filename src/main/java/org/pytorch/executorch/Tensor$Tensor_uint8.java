/*
 * Decompiled with CFR 0.152.
 */
package org.pytorch.executorch;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.pytorch.executorch.DType;
import org.pytorch.executorch.Tensor;

static class Tensor.Tensor_uint8
extends Tensor {
    private final ByteBuffer data;

    private Tensor.Tensor_uint8(ByteBuffer data, long[] shape) {
        super(shape, null);
        this.data = data;
    }

    @Override
    public DType dtype() {
        return DType.UINT8;
    }

    @Override
    Buffer getRawDataBuffer() {
        return this.data;
    }

    @Override
    public byte[] getDataAsUnsignedByteArray() {
        this.data.rewind();
        byte[] arr = new byte[this.data.remaining()];
        this.data.get(arr);
        return arr;
    }

    public String toString() {
        return String.format("Tensor(%s, dtype=torch.uint8)", Arrays.toString(this.shape));
    }
}
