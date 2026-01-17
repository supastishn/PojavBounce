package org.pytorch.executorch;

import com.facebook.jni.annotations.DoNotStrip;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import org.pytorch.executorch.annotations.Experimental;

@DoNotStrip
@Experimental
/* loaded from: executorch-android-1.0.1.aar:classes.jar:org/pytorch/executorch/EValue.class */
public class EValue {
    private static final int TYPE_CODE_NONE = 0;
    private static final int TYPE_CODE_TENSOR = 1;
    private static final int TYPE_CODE_STRING = 2;
    private static final int TYPE_CODE_DOUBLE = 3;
    private static final int TYPE_CODE_INT = 4;
    private static final int TYPE_CODE_BOOL = 5;
    private String[] TYPE_NAMES = {"None", "Tensor", "String", "Double", "Int", "Bool"};

    @DoNotStrip
    private final int mTypeCode;

    @DoNotStrip
    private Object mData;

    @DoNotStrip
    private EValue(int typeCode) {
        this.mTypeCode = typeCode;
    }

    @DoNotStrip
    public boolean isNone() {
        return 0 == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isTensor() {
        return 1 == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isBool() {
        return TYPE_CODE_BOOL == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isInt() {
        return TYPE_CODE_INT == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isDouble() {
        return 3 == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isString() {
        return 2 == this.mTypeCode;
    }

    @DoNotStrip
    public static EValue optionalNone() {
        return new EValue(0);
    }

    @DoNotStrip
    public static EValue from(Tensor tensor) {
        EValue iv = new EValue(1);
        iv.mData = tensor;
        return iv;
    }

    @DoNotStrip
    public static EValue from(boolean value) {
        EValue iv = new EValue(TYPE_CODE_BOOL);
        iv.mData = Boolean.valueOf(value);
        return iv;
    }

    @DoNotStrip
    public static EValue from(long value) {
        EValue iv = new EValue(TYPE_CODE_INT);
        iv.mData = Long.valueOf(value);
        return iv;
    }

    @DoNotStrip
    public static EValue from(double value) {
        EValue iv = new EValue(3);
        iv.mData = Double.valueOf(value);
        return iv;
    }

    @DoNotStrip
    public static EValue from(String value) {
        EValue iv = new EValue(2);
        iv.mData = value;
        return iv;
    }

    @DoNotStrip
    public Tensor toTensor() {
        preconditionType(1, this.mTypeCode);
        return (Tensor) this.mData;
    }

    @DoNotStrip
    public boolean toBool() {
        preconditionType(TYPE_CODE_BOOL, this.mTypeCode);
        return ((Boolean) this.mData).booleanValue();
    }

    @DoNotStrip
    public long toInt() {
        preconditionType(TYPE_CODE_INT, this.mTypeCode);
        return ((Long) this.mData).longValue();
    }

    @DoNotStrip
    public double toDouble() {
        preconditionType(3, this.mTypeCode);
        return ((Double) this.mData).doubleValue();
    }

    @DoNotStrip
    public String toStr() {
        preconditionType(2, this.mTypeCode);
        return (String) this.mData;
    }

    private void preconditionType(int typeCodeExpected, int typeCode) {
        if (typeCode != typeCodeExpected) {
            throw new IllegalStateException(String.format(Locale.US, "Expected EValue type %s, actual type %s", getTypeName(typeCodeExpected), getTypeName(typeCode)));
        }
    }

    private String getTypeName(int typeCode) {
        return (typeCode < 0 || typeCode >= this.TYPE_NAMES.length) ? "Unknown" : this.TYPE_NAMES[typeCode];
    }

    public byte[] toByteArray() {
        if (isNone()) {
            return ByteBuffer.allocate(1).put((byte) 0).array();
        }
        if (isTensor()) {
            Tensor t = toTensor();
            byte[] tByteArray = t.toByteArray();
            return ByteBuffer.allocate(1 + tByteArray.length).put((byte) 1).put(tByteArray).array();
        }
        if (isBool()) {
            return ByteBuffer.allocate(2).put((byte) 5).put((byte) (toBool() ? 1 : 0)).array();
        }
        if (isInt()) {
            return ByteBuffer.allocate(9).put((byte) 4).putLong(toInt()).array();
        }
        if (isDouble()) {
            return ByteBuffer.allocate(9).put((byte) 3).putDouble(toDouble()).array();
        }
        if (isString()) {
            return ByteBuffer.allocate(1 + toString().length()).put((byte) 2).put(toString().getBytes()).array();
        }
        throw new IllegalArgumentException("Unknown Tensor dtype");
    }

    public static EValue fromByteArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null");
        }
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("invalid buffer");
        }
        int typeCode = buffer.get();
        switch (typeCode) {
            case 0:
                return new EValue(0);
            case 1:
                byte[] bufferArray = buffer.array();
                return from(Tensor.fromByteArray(Arrays.copyOfRange(bufferArray, 1, bufferArray.length)));
            case 2:
                throw new IllegalArgumentException("TYPE_CODE_STRING is not supported");
            case 3:
                return from(buffer.getDouble());
            case TYPE_CODE_INT /* 4 */:
                return from(buffer.getLong());
            case TYPE_CODE_BOOL /* 5 */:
                return from(buffer.get() != 0);
            default:
                throw new IllegalArgumentException("invalid type code: " + typeCode);
        }
    }
}
