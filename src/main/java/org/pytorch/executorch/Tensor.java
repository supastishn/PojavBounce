/*
 * Stub implementation of org.pytorch.executorch.Tensor
 * Represents tensor data that can be passed to/from ExecuTorch models
 */
package org.pytorch.executorch;

public class Tensor {
    private final long[] shape;
    private final Object data;
    private final DType dtype;

    public enum DType {
        FLOAT32,
        INT32,
        INT64,
        FLOAT64,
        INT8,
        UINT8
    }

    public Tensor(long[] shape, Object data, DType dtype) {
        this.shape = shape;
        this.data = data;
        this.dtype = dtype;
    }

    public long[] getShape() {
        return shape;
    }

    public Object getData() {
        return data;
    }

    public DType getDtype() {
        return dtype;
    }
}
