/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.facebook.jni.annotations.DoNotStrip
 */
package org.pytorch.executorch;

import com.facebook.jni.annotations.DoNotStrip;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import org.pytorch.executorch.Tensor;
import org.pytorch.executorch.annotations.Experimental;

@Experimental
@DoNotStrip
public class EValue {
    private static final int TYPE_CODE_NONE = 0;
    private static final int TYPE_CODE_TENSOR = 1;
    private static final int TYPE_CODE_STRING = 2;
    private static final int TYPE_CODE_DOUBLE = 3;
    private static final int TYPE_CODE_INT = 4;
    private static final int TYPE_CODE_BOOL = 5;
    private String[] TYPE_NAMES = new String[]{"None", "Tensor", "String", "Double", "Int", "Bool"};
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
        return 5 == this.mTypeCode;
    }

    @DoNotStrip
    public boolean isInt() {
        return 4 == this.mTypeCode;
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
        EValue iv = new EValue(5);
        iv.mData = value;
        return iv;
    }

    @DoNotStrip
    public static EValue from(long value) {
        EValue iv = new EValue(4);
        iv.mData = value;
        return iv;
    }

    @DoNotStrip
    public static EValue from(double value) {
        EValue iv = new EValue(3);
        iv.mData = value;
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
        this.preconditionType(1, this.mTypeCode);
        return (Tensor)this.mData;
    }

    @DoNotStrip
    public boolean toBool() {
        this.preconditionType(5, this.mTypeCode);
        return (Boolean)this.mData;
    }

    @DoNotStrip
    public long toInt() {
        this.preconditionType(4, this.mTypeCode);
        return (Long)this.mData;
    }

    @DoNotStrip
    public double toDouble() {
        this.preconditionType(3, this.mTypeCode);
        return (Double)this.mData;
    }

    @DoNotStrip
    public String toStr() {
        this.preconditionType(2, this.mTypeCode);
        return (String)this.mData;
    }

    private void preconditionType(int typeCodeExpected, int typeCode) {
        if (typeCode != typeCodeExpected) {
            throw new IllegalStateException(String.format(Locale.US, "Expected EValue type %s, actual type %s", this.getTypeName(typeCodeExpected), this.getTypeName(typeCode)));
        }
    }

    private String getTypeName(int typeCode) {
        return typeCode >= 0 && typeCode < this.TYPE_NAMES.length ? this.TYPE_NAMES[typeCode] : "Unknown";
    }

    public byte[] toByteArray() {
        if (this.isNone()) {
            return ByteBuffer.allocate(1).put((byte)0).array();
        }
        if (this.isTensor()) {
            Tensor t = this.toTensor();
            byte[] tByteArray = t.toByteArray();
            return ByteBuffer.allocate(1 + tByteArray.length).put((byte)1).put(tByteArray).array();
        }
        if (this.isBool()) {
            return ByteBuffer.allocate(2).put((byte)5).put((byte)(this.toBool() ? 1 : 0)).array();
        }
        if (this.isInt()) {
            return ByteBuffer.allocate(9).put((byte)4).putLong(this.toInt()).array();
        }
        if (this.isDouble()) {
            return ByteBuffer.allocate(9).put((byte)3).putDouble(this.toDouble()).array();
        }
        if (this.isString()) {
            return ByteBuffer.allocate(1 + this.toString().length()).put((byte)2).put(this.toString().getBytes()).array();
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
        byte typeCode = buffer.get();
        switch (typeCode) {
            case 0: {
                return new EValue(0);
            }
            case 1: {
                byte[] bufferArray = buffer.array();
                return EValue.from(Tensor.fromByteArray(Arrays.copyOfRange(bufferArray, 1, bufferArray.length)));
            }
            case 2: {
                throw new IllegalArgumentException("TYPE_CODE_STRING is not supported");
            }
            case 3: {
                return EValue.from(buffer.getDouble());
            }
            case 4: {
                return EValue.from(buffer.getLong());
            }
            case 5: {
                return EValue.from(buffer.get() != 0);
            }
        }
        throw new IllegalArgumentException("invalid type code: " + typeCode);
    }
}
