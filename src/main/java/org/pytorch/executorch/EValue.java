/*
 * Stub implementation of org.pytorch.executorch.EValue
 * Represents a value that can be passed to/from ExecuTorch
 */
package org.pytorch.executorch;

public class EValue {
    private final Object value;
    private final Type type;

    public enum Type {
        NONE,
        TENSOR,
        INT,
        DOUBLE,
        BOOL,
        STRING,
        LIST_TENSOR,
        LIST_INT,
        LIST_DOUBLE,
        LIST_BOOL
    }

    public EValue(Object value, Type type) {
        this.value = value;
        this.type = type;
    }

    public static EValue from(Object obj) {
        if (obj instanceof Tensor) {
            return new EValue(obj, Type.TENSOR);
        } else if (obj instanceof Integer || obj instanceof Long) {
            return new EValue(obj, Type.INT);
        } else if (obj instanceof Double || obj instanceof Float) {
            return new EValue(obj, Type.DOUBLE);
        } else if (obj instanceof Boolean) {
            return new EValue(obj, Type.BOOL);
        } else if (obj instanceof String) {
            return new EValue(obj, Type.STRING);
        } else {
            return new EValue(obj, Type.NONE);
        }
    }

    public Object getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public boolean isTensor() {
        return type == Type.TENSOR;
    }

    public Tensor toTensor() {
        if (type == Type.TENSOR) {
            return (Tensor) value;
        }
        throw new IllegalStateException("EValue is not a tensor");
    }
}
